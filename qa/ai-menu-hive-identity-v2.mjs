import { createHash } from "node:crypto";

const HASH_PATTERN = /^[a-f0-9]{64}$/;
const SUPPORTED_LANGUAGES = new Set(["SPANISH", "ENGLISH", "FRENCH"]);
const COMPONENT_ALIASES = new Map([
  ["spaghetti", "pasta"],
  ["espagueti", "pasta"],
  ["espaguetis", "pasta"],
  ["macaroni", "pasta"],
  ["macarron", "pasta"],
  ["macarrones", "pasta"],
  ["tomate", "tomato"],
  ["tomates", "tomato"],
  ["lenteja", "lentils"],
  ["lentejas", "lentils"],
  ["lentil", "lentils"],
  ["vegetable", "vegetables"],
  ["verdura", "vegetables"],
  ["verduras", "vegetables"],
  ["salsa", "sauce"],
  ["salsa de tomate", "sauce"]
]);
const PREPARATION_ALIASES = new Map([
  ["mix", "mixed"],
  ["mixing", "mixed"],
  ["toss", "mixed"],
  ["tossed", "mixed"],
  ["mezcla", "mixed"],
  ["mezclado", "mixed"],
  ["mezclada", "mixed"]
]);

export function canonicalIdentity(language, rawKey) {
  if (!SUPPORTED_LANGUAGES.has(language)) return null;
  const rawParts = String(rawKey ?? "").split("|");
  if (rawParts.length !== 3) return null;
  const parts = [
    canonicalComponent(rawParts[0]),
    canonicalIngredients(rawParts[1]),
    canonicalPreparation(rawParts[2])
  ];
  if (parts.some((part) => part.length === 0)) return null;
  const canonicalKey = parts.join("|");
  return {
    canonicalKey,
    semanticHash: createHash("sha256")
      .update(`${language}|${canonicalKey}`, "utf8")
      .digest("hex")
  };
}

export function buildMigrationPlan(documents) {
  const groups = new Map();
  const skipped = [];

  for (const document of documents) {
    const reason = invalidDocumentReason(document);
    if (reason) {
      skipped.push({ id: document?.id ?? "", reason });
      continue;
    }
    const identity = canonicalIdentity(
      document.data.language,
      document.data.semanticKey
    );
    if (!identity) {
      skipped.push({ id: document.id, reason: "invalid semantic identity" });
      continue;
    }
    const group = groups.get(identity.semanticHash) ?? [];
    group.push({ ...document, identity });
    groups.set(identity.semanticHash, group);
  }

  const writes = [];
  const deletes = [];
  for (const [semanticHash, members] of groups.entries()) {
    const ordered = [...members].sort(compareCreatedAtThenId);
    const representative = ordered[0];
    const eligibilityKeys = [...new Set(
      ordered.flatMap(({ data }) => data.eligibilityKeys)
    )].sort();
    if (eligibilityKeys.length > 100) {
      skipped.push(
        ...ordered.map(({ id }) => ({
          id,
          reason: "merged eligibility keys exceed 100"
        }))
      );
      continue;
    }
    writes.push({
      id: semanticHash,
      sourceIds: ordered.map(({ id }) => id),
      data: {
        ...representative.data,
        identityVersion: 2,
        semanticHash,
        semanticKey: representative.identity.canonicalKey,
        eligibilityKeys
      }
    });
    deletes.push(
      ...ordered
        .map(({ id }) => id)
        .filter((id) => id !== semanticHash)
    );
  }

  return {
    writes: writes.sort((left, right) => left.id.localeCompare(right.id)),
    deletes: [...new Set(deletes)].sort(),
    skipped: skipped.sort((left, right) => left.id.localeCompare(right.id))
  };
}

function canonicalComponent(value) {
  const normalized = normalize(value);
  return COMPONENT_ALIASES.get(normalized) ?? normalized;
}

function canonicalIngredients(value) {
  return [...new Set(
    String(value ?? "")
      .split(/[+,]/)
      .map(canonicalComponent)
      .filter(Boolean)
  )].sort().join("+");
}

function canonicalPreparation(value) {
  const normalized = normalize(value);
  return PREPARATION_ALIASES.get(normalized) ?? canonicalComponent(normalized);
}

function normalize(value) {
  return String(value ?? "")
    .normalize("NFD")
    .replace(/\p{M}+/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9 ]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function invalidDocumentReason(document) {
  if (!document || typeof document.id !== "string" || !document.data) {
    return "invalid document envelope";
  }
  if (!HASH_PATTERN.test(document.id)) return "invalid document id";
  if (document.data.schemaVersion !== 1) return "unsupported schema version";
  if (document.data.semanticHash !== document.id) return "semantic hash mismatch";
  if (!SUPPORTED_LANGUAGES.has(document.data.language)) return "unsupported language";
  if (
    !Array.isArray(document.data.eligibilityKeys) ||
    document.data.eligibilityKeys.length === 0 ||
    !document.data.eligibilityKeys.every((key) => HASH_PATTERN.test(key))
  ) {
    return "invalid eligibility keys";
  }
  return null;
}

function compareCreatedAtThenId(left, right) {
  const timeDifference = timestampMillis(left.data.createdAt) -
    timestampMillis(right.data.createdAt);
  return timeDifference || left.id.localeCompare(right.id);
}

function timestampMillis(value) {
  if (typeof value === "number") return value;
  if (value instanceof Date) return value.getTime();
  if (typeof value?.toMillis === "function") return value.toMillis();
  if (Number.isFinite(value?._seconds)) {
    return value._seconds * 1000 + (value._nanoseconds ?? 0) / 1_000_000;
  }
  return Number.MAX_SAFE_INTEGER;
}
