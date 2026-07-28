import { readFileSync } from "node:fs";
import { test } from "node:test";
import assert from "node:assert/strict";
import {
  buildMigrationPlan,
  canonicalIdentity
} from "./ai-menu-hive-identity-v2.mjs";
import { parseMigrationArguments } from "./migrate-ai-menu-hive-v2.mjs";

const FIXTURE_URL = new URL(
  "../app/src/test/resources/ai-menu-hive-identity-v2-fixtures.tsv",
  import.meta.url
);
const FIXTURES = readFileSync(FIXTURE_URL, "utf8")
  .trim()
  .split("\n")
  .map((line) => {
    const [language, rawKey, canonicalKey] = line.split("\t");
    return { language, rawKey, canonicalKey };
  });

test("shared Kotlin fixtures produce the declared v2 identity", () => {
  const identities = FIXTURES.map(({ language, rawKey, canonicalKey }) => {
    const identity = canonicalIdentity(language, rawKey);
    assert.equal(identity?.canonicalKey, canonicalKey);
    return identity;
  });

  assert.equal(new Set(identities.map(({ semanticHash }) => semanticHash)).size, 1);
});

test("migration merges reported duplicates without mutating input", () => {
  const ids = [
    "10baf0eeb930709f2997a9c2ece167e0895e64d7c39f54d05514e0f4f35ef3c2",
    "6f70f92ed9ba1b5697b64921edf2f222c799ef977654e5d53c605a08ebe66290",
    "ecaee3ff4a9fa68ab3fee0bb44d69a5e9e82cd3e46845205914167724d77879e"
  ];
  const documents = FIXTURES.map((fixture, index) => ({
    id: ids[index],
    data: sampleDocument({
      semanticHash: ids[index],
      semanticKey: fixture.rawKey,
      createdAt: index + 1,
      eligibilityKeys: [String(index + 1).repeat(64)]
    })
  }));
  const snapshot = structuredClone(documents);

  const plan = buildMigrationPlan(documents);

  assert.deepEqual(documents, snapshot);
  assert.equal(plan.writes.length, 1);
  assert.equal(plan.deletes.length, 2);
  assert.deepEqual(plan.deletes, ids.slice(0, 2));
  assert.equal(
    plan.writes[0].id,
    "ecaee3ff4a9fa68ab3fee0bb44d69a5e9e82cd3e46845205914167724d77879e"
  );
  assert.equal(plan.writes[0].data.identityVersion, 2);
  assert.equal(
    plan.writes[0].data.semanticKey,
    "salad|lentils+vegetables|mixed"
  );
  assert.deepEqual(
    plan.writes[0].data.eligibilityKeys,
    ["1".repeat(64), "2".repeat(64), "3".repeat(64)]
  );
  assert.equal(plan.writes[0].data.name, "Receta 1");
  assert.deepEqual(plan.skipped, []);
});

test("different recipes stay in separate migration groups", () => {
  const documents = [
    legacyDocument("a".repeat(64), "salad|lentils+vegetables|mixed", 1),
    legacyDocument("b".repeat(64), "stew|lentils+vegetables|stewed", 2)
  ];

  const plan = buildMigrationPlan(documents);

  assert.equal(plan.writes.length, 2);
  assert.equal(plan.deletes.length, 2);
});

test("invalid identity is skipped instead of being deleted", () => {
  const invalid = {
    id: "a".repeat(64),
    data: sampleDocument({
      semanticHash: "a".repeat(64),
      semanticKey: "not a structured key",
      createdAt: 1,
      eligibilityKeys: ["1".repeat(64)]
    })
  };

  const plan = buildMigrationPlan([invalid]);

  assert.deepEqual(plan.writes, []);
  assert.deepEqual(plan.deletes, []);
  assert.deepEqual(plan.skipped, [
    { id: invalid.id, reason: "invalid semantic identity" }
  ]);
});

test("migration CLI is dry run by default and guards apply mode", () => {
  assert.deepEqual(
    parseMigrationArguments(["--project=menudado-6a2da"]),
    {
      projectId: "menudado-6a2da",
      apply: false,
      backupPath: null
    }
  );
  assert.throws(
    () => parseMigrationArguments([]),
    /--project is required/
  );
  assert.throws(
    () => parseMigrationArguments([
      "--project=menudado-6a2da",
      "--apply"
    ]),
    /--confirm-project/
  );
  assert.deepEqual(
    parseMigrationArguments([
      "--project=menudado-6a2da",
      "--apply",
      "--confirm-project=menudado-6a2da",
      "--backup=/tmp/hive-backup.json"
    ]),
    {
      projectId: "menudado-6a2da",
      apply: true,
      backupPath: "/tmp/hive-backup.json"
    }
  );
});

function legacyDocument(id, semanticKey, createdAt) {
  return {
    id,
    data: sampleDocument({
      semanticHash: id,
      semanticKey,
      createdAt,
      eligibilityKeys: [createdAt.toString().repeat(64)]
    })
  };
}

function sampleDocument({
  semanticHash,
  semanticKey,
  createdAt,
  eligibilityKeys
}) {
  return {
    schemaVersion: 1,
    semanticHash,
    semanticKey,
    language: "SPANISH",
    name: `Receta ${createdAt}`,
    description: "Descripción válida.",
    notes: "Lista en 15 minutos.",
    calories: 450,
    healthStatus: "HEALTHY",
    healthReason: "Motivo.",
    healthSuggestion: "Sugerencia.",
    cuisineInspiration: "MEDITERRANEAN",
    shoppingProducts: [],
    eligibilityKeys,
    createdAt,
    updatedAt: createdAt
  };
}
