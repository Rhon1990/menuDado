import { mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { pathToFileURL } from "node:url";
import { buildMigrationPlan } from "./ai-menu-hive-identity-v2.mjs";

const COLLECTION = "sharedAiMenus";

export function parseMigrationArguments(args) {
  let projectId = null;
  let confirmProject = null;
  let apply = false;
  let backupPath = null;

  for (const argument of args) {
    if (argument.startsWith("--project=")) {
      projectId = argument.slice("--project=".length);
    } else if (argument.startsWith("--confirm-project=")) {
      confirmProject = argument.slice("--confirm-project=".length);
    } else if (argument.startsWith("--backup=")) {
      backupPath = argument.slice("--backup=".length);
    } else if (argument === "--apply") {
      apply = true;
    } else {
      throw new Error(`Unknown argument: ${argument}`);
    }
  }

  if (!projectId) throw new Error("--project is required");
  if (!/^[a-z0-9-]+$/.test(projectId)) {
    throw new Error("--project must be an exact Firebase project ID");
  }
  if (apply && confirmProject !== projectId) {
    throw new Error("--confirm-project must exactly match --project in apply mode");
  }

  return { projectId, apply, backupPath };
}

async function main() {
  const options = parseMigrationArguments(process.argv.slice(2));
  const { FieldValue, Firestore } = await import("@google-cloud/firestore");
  const firestore = new Firestore({
    projectId: options.projectId
  });

  try {
    const snapshot = await firestore.collection(COLLECTION).get();
    const documents = snapshot.docs.map((document) => ({
      id: document.id,
      data: document.data()
    }));
    const plan = buildMigrationPlan(documents);
    printPlan(options, documents.length, plan);

    if (!options.apply) {
      console.log("DRY RUN: no Firestore writes or deletes were performed.");
      return;
    }

    const backupPath = options.backupPath ?? join(
      tmpdir(),
      `menudado-shared-ai-menus-v2-${Date.now()}.json`
    );
    await mkdir(dirname(backupPath), { recursive: true });
    await writeFile(
      backupPath,
      JSON.stringify(documents, firestoreJsonReplacer, 2),
      "utf8"
    );
    console.log(`Backup written before changes: ${backupPath}`);

    for (const write of plan.writes) {
      const reference = firestore.collection(COLLECTION).doc(write.id);
      await reference.set({
        ...write.data,
        identityVersion: 2,
        updatedAt: FieldValue.serverTimestamp()
      });
      const verified = await reference.get();
      validateAppliedDocument(write, verified);

      const duplicateIds = write.sourceIds.filter((id) => id !== write.id);
      for (const duplicateId of duplicateIds) {
        await firestore.collection(COLLECTION).doc(duplicateId).delete();
      }
    }

    console.log(
      `APPLIED: ${plan.writes.length} canonical documents written, ` +
      `${plan.deletes.length} duplicate documents deleted.`
    );
  } finally {
    await firestore.terminate();
  }
}

function printPlan(options, documentCount, plan) {
  console.log(`Project: ${options.projectId}`);
  console.log(`Mode: ${options.apply ? "APPLY" : "DRY RUN"}`);
  console.log(`Documents read: ${documentCount}`);
  console.log(`Canonical writes: ${plan.writes.length}`);
  console.log(`Duplicate deletes: ${plan.deletes.length}`);
  console.log(`Skipped documents: ${plan.skipped.length}`);
  console.log(`Similarity review groups: ${plan.reviewGroups.length}`);
  for (const write of plan.writes) {
    console.log(
      `- ${write.id}: ${write.sourceIds.length} source document(s), ` +
      `${write.data.eligibilityKeys.length} eligibility key(s)`
    );
  }
  for (const skipped of plan.skipped) {
    console.log(`- SKIPPED ${skipped.id}: ${skipped.reason}`);
  }
  for (const group of plan.reviewGroups) {
    console.log(
      `- REVIEW ONLY ${group.ids.join(", ")} ` +
      `(similarity ${group.similarity.toFixed(2)}; no automatic delete)`
    );
  }
}

function validateAppliedDocument(write, snapshot) {
  if (!snapshot.exists) {
    throw new Error(`Canonical document was not persisted: ${write.id}`);
  }
  const stored = snapshot.data();
  const storedKeys = [...(stored.eligibilityKeys ?? [])].sort();
  if (
    stored.identityVersion !== 2 ||
    stored.semanticHash !== write.id ||
    stored.semanticKey !== write.data.semanticKey ||
    JSON.stringify(storedKeys) !== JSON.stringify(write.data.eligibilityKeys)
  ) {
    throw new Error(`Canonical document verification failed: ${write.id}`);
  }
}

function firestoreJsonReplacer(_key, value) {
  if (typeof value?.toDate === "function") {
    return value.toDate().toISOString();
  }
  return value;
}

const invokedPath = process.argv[1]
  ? pathToFileURL(process.argv[1]).href
  : null;
if (invokedPath === import.meta.url) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : error);
    process.exitCode = 1;
  });
}
