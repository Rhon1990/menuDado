import { readFileSync } from "node:fs";
import { after, afterEach, before, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from "@firebase/rules-unit-testing";
import {
  Timestamp,
  deleteDoc,
  doc,
  getDoc,
  setDoc,
  updateDoc
} from "firebase/firestore";

const PROJECT_ID = "menudado-rules-test";
const HASH = "a".repeat(64);
const ELIGIBILITY_HASH = "b".repeat(64);
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8")
    }
  });
});

afterEach(async () => testEnv.clearFirestore());
after(async () => testEnv.cleanup());

function validDocument() {
  const now = Timestamp.now();
  return {
    schemaVersion: 1,
    semanticHash: HASH,
    semanticKey: "pasta|tomato|sauce",
    language: "SPANISH",
    name: "Pasta con tomate",
    description: "Pasta integral con salsa de tomate.",
    notes: "Lista en 10 minutos.",
    calories: 430,
    healthStatus: "HEALTHY",
    healthReason: "Incluye cereal y tomate.",
    healthSuggestion: "Acompaña con verduras.",
    cuisineInspiration: "ITALIAN",
    shoppingProducts: [
      {
        key: "pasta-integral",
        normalizedName: "pasta integral",
        displayName: "Pasta integral"
      }
    ],
    eligibilityKeys: [ELIGIBILITY_HASH],
    createdAt: now,
    updatedAt: now
  };
}

test("authenticated user can create and read sanitized hive menu", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  await assertSucceeds(setDoc(doc(db, "sharedAiMenus", HASH), validDocument()));
  await assertSucceeds(getDoc(doc(db, "sharedAiMenus", HASH)));
});

test("unauthenticated user cannot read hive", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "sharedAiMenus", HASH)));
});

test("private menus remain isolated by uid", async () => {
  const ownerDb = testEnv.authenticatedContext("owner").firestore();
  const otherDb = testEnv.authenticatedContext("other").firestore();
  await assertSucceeds(setDoc(doc(ownerDb, "users/owner/menus/1"), { name: "Privado" }));
  await assertFails(getDoc(doc(otherDb, "users/owner/menus/1")));
});

test("identity and unknown fields are rejected", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  await assertFails(
    setDoc(doc(db, "sharedAiMenus", HASH), { ...validDocument(), uid: "user-a" })
  );
});

test("recipe overwrite and delete are rejected", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  await assertSucceeds(setDoc(doc(db, "sharedAiMenus", HASH), validDocument()));
  await assertFails(updateDoc(doc(db, "sharedAiMenus", HASH), { name: "Alterado" }));
  await assertFails(deleteDoc(doc(db, "sharedAiMenus", HASH)));
});

test("invalid eligibility hash is rejected on update", async () => {
  const db = testEnv.authenticatedContext("user-a").firestore();
  await assertSucceeds(setDoc(doc(db, "sharedAiMenus", HASH), validDocument()));
  await assertFails(
    updateDoc(doc(db, "sharedAiMenus", HASH), {
      eligibilityKeys: [ELIGIBILITY_HASH, "invalid"]
    })
  );
});
