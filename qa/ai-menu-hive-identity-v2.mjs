import { createHash } from "node:crypto";

const HASH_PATTERN = /^[a-f0-9]{64}$/;
const SIMILARITY_THRESHOLD = 0.80;
const SUPPORTED_LANGUAGES = new Set(["SPANISH", "ENGLISH", "FRENCH"]);
const COMPONENT_ALIASES = new Map([
  ["spaghetti", "pasta"],
  ["espagueti", "pasta"],
  ["espaguetis", "pasta"],
  ["macaroni", "pasta"],
  ["macarron", "pasta"],
  ["macarrones", "pasta"],
  ["toast", "toast"],
  ["toasts", "toast"],
  ["tostada", "toast"],
  ["tostadas", "toast"],
  ["tartine", "toast"],
  ["tartines", "toast"],
  ["salad", "salad"],
  ["salads", "salad"],
  ["ensalada", "salad"],
  ["ensaladas", "salad"],
  ["salade", "salad"],
  ["salades", "salad"],
  ["soup", "soup"],
  ["soups", "soup"],
  ["sopa", "soup"],
  ["sopas", "soup"],
  ["soupe", "soup"],
  ["soupes", "soup"],
  ["stew", "stew"],
  ["stews", "stew"],
  ["guiso", "stew"],
  ["guisos", "stew"],
  ["estofado", "stew"],
  ["estofados", "stew"],
  ["ragout", "stew"],
  ["ragouts", "stew"],
  ["bowl", "bowl"],
  ["bowls", "bowl"],
  ["bol", "bowl"],
  ["boles", "bowl"],
  ["bols", "bowl"],
  ["sandwich", "sandwich"],
  ["sandwiches", "sandwich"],
  ["bocadillo", "sandwich"],
  ["bocadillos", "sandwich"],
  ["omelette", "omelette"],
  ["omelettes", "omelette"],
  ["omelet", "omelette"],
  ["omelets", "omelette"],
  ["tortilla francesa", "omelette"],
  ["tomate", "tomato"],
  ["tomates", "tomato"],
  ["lenteja", "lentils"],
  ["lentejas", "lentils"],
  ["lentil", "lentils"],
  ["vegetable", "vegetables"],
  ["verdura", "vegetables"],
  ["verduras", "vegetables"],
  ["avocados", "avocado"],
  ["aguacate", "avocado"],
  ["aguacates", "avocado"],
  ["avocat", "avocado"],
  ["avocats", "avocado"],
  ["eggs", "egg"],
  ["huevo", "egg"],
  ["huevos", "egg"],
  ["oeuf", "egg"],
  ["oeufs", "egg"],
  ["pollo", "chicken"],
  ["pollos", "chicken"],
  ["poulet", "chicken"],
  ["poulets", "chicken"],
  ["arroz", "rice"],
  ["riz", "rice"],
  ["chickpea", "chickpeas"],
  ["garbanzo", "chickpeas"],
  ["garbanzos", "chickpeas"],
  ["pois chiche", "chickpeas"],
  ["pois chiches", "chickpeas"],
  ["potatoes", "potato"],
  ["patata", "potato"],
  ["patatas", "potato"],
  ["pomme de terre", "potato"],
  ["pommes de terre", "potato"],
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
  ["mezclada", "mixed"],
  ["melange", "mixed"],
  ["melangee", "mixed"],
  ["assembled", "assembled"],
  ["mounted", "assembled"],
  ["montado", "assembled"],
  ["montada", "assembled"],
  ["monte", "assembled"],
  ["montee", "assembled"],
  ["fried", "fried"],
  ["frito", "fried"],
  ["frita", "fried"],
  ["frit", "fried"],
  ["frite", "fried"],
  ["baked", "baked"],
  ["horneado", "baked"],
  ["horneada", "baked"],
  ["au four", "baked"],
  ["grilled", "grilled"],
  ["a la plancha", "grilled"],
  ["plancha", "grilled"],
  ["parrilla", "grilled"],
  ["grille", "grilled"],
  ["grillee", "grilled"],
  ["boiled", "boiled"],
  ["cocido", "boiled"],
  ["cocida", "boiled"],
  ["hervido", "boiled"],
  ["hervida", "boiled"],
  ["bouilli", "boiled"],
  ["bouillie", "boiled"],
  ["stewed", "stewed"],
  ["guisado", "stewed"],
  ["guisada", "stewed"],
  ["estofado", "stewed"],
  ["estofada", "stewed"],
  ["mijote", "stewed"],
  ["mijotee", "stewed"],
  ["roasted", "roasted"],
  ["asado", "roasted"],
  ["asada", "roasted"],
  ["roti", "roasted"],
  ["rotie", "roasted"]
]);
const CONCEPT_ALIASES = new Map([
  ...COMPONENT_ALIASES,
  ...PREPARATION_ALIASES,
  ["stewed", "stew"],
  ["guisado", "stew"],
  ["guisada", "stew"],
  ["estofado", "stew"],
  ["estofada", "stew"],
  ["mijote", "stew"],
  ["mijotee", "stew"]
]);

export function canonicalIdentity(language, rawKey) {
  if (!SUPPORTED_LANGUAGES.has(language)) return null;
  const parts = canonicalParts(rawKey);
  if (!parts) return null;
  const canonicalKey = parts.join("|");
  return {
    canonicalKey,
    semanticHash: createHash("sha256")
      .update(`${language}|${canonicalKey}`, "utf8")
      .digest("hex")
  };
}

export function conceptSignature(rawKey) {
  const rawParts = String(rawKey ?? "").split("|");
  if (rawParts.length !== 3) return null;
  const family = canonicalSegment(rawParts[0], canonicalComponent);
  const concepts = new Set(
    rawParts
      .flatMap((part) => part.split(/[+,]/))
      .map(canonicalConcept)
      .filter(Boolean)
  );
  if (!family || concepts.size === 0) return null;
  return {
    family,
    concepts
  };
}

export function identitySimilarity(firstKey, secondKey) {
  const first = conceptSignature(firstKey);
  const second = conceptSignature(secondKey);
  if (!first || !second) return 0;
  if (setsEqual(first.concepts, second.concepts)) return 1;
  if (first.family !== second.family) return 0;
  const union = new Set([...first.concepts, ...second.concepts]);
  if (union.size === 0) return 0;
  const intersectionSize = [...first.concepts]
    .filter((concept) => second.concepts.has(concept))
    .length;
  return intersectionSize / union.size;
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

  const sortedWrites = writes.sort((left, right) => left.id.localeCompare(right.id));
  return {
    writes: sortedWrites,
    deletes: [...new Set(deletes)].sort(),
    skipped: skipped.sort((left, right) => left.id.localeCompare(right.id)),
    reviewGroups: similarityReviewGroups(sortedWrites)
  };
}

export function refreshMigrationWrite(plannedWrite, currentDocuments) {
  const refreshedPlan = buildMigrationPlan(currentDocuments);
  const refreshedWrite = refreshedPlan.writes
    .find(({ id }) => id === plannedWrite.id);
  if (
    refreshedPlan.skipped.length > 0 ||
    refreshedPlan.writes.length !== 1 ||
    !refreshedWrite
  ) {
    throw new Error(`Migration group changed while applying ${plannedWrite.id}`);
  }
  return refreshedWrite;
}

function canonicalParts(rawKey) {
  const rawParts = String(rawKey ?? "").split("|");
  if (rawParts.length !== 3) return null;
  const parts = [
    canonicalSegment(rawParts[0], canonicalComponent),
    canonicalSegment(rawParts[1], canonicalComponent),
    canonicalSegment(rawParts[2], canonicalPreparation)
  ];
  return parts.some((part) => part.length === 0) ? null : parts;
}

function canonicalSegment(value, canonicalizer) {
  return [...new Set(
    String(value ?? "")
      .split(/[+,]/)
      .map(canonicalizer)
      .filter(Boolean)
  )].sort().join("+");
}

function canonicalComponent(value) {
  const normalized = normalize(value);
  return COMPONENT_ALIASES.get(normalized) ?? normalized;
}

function canonicalPreparation(value) {
  const normalized = normalize(value);
  return PREPARATION_ALIASES.get(normalized) ?? canonicalComponent(normalized);
}

function canonicalConcept(value) {
  const normalized = normalize(value);
  return CONCEPT_ALIASES.get(normalized) ?? normalized;
}

function normalize(value) {
  return String(value ?? "")
    .toLowerCase()
    .replaceAll("œ", "oe")
    .replaceAll("æ", "ae")
    .normalize("NFD")
    .replace(/\p{M}+/gu, "")
    .replace(/[^a-z0-9 ]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function similarityReviewGroups(writes) {
  const groups = [];
  for (let firstIndex = 0; firstIndex < writes.length; firstIndex += 1) {
    for (
      let secondIndex = firstIndex + 1;
      secondIndex < writes.length;
      secondIndex += 1
    ) {
      const first = writes[firstIndex];
      const second = writes[secondIndex];
      const similarity = identitySimilarity(
        first.data.semanticKey,
        second.data.semanticKey
      );
      if (similarity >= SIMILARITY_THRESHOLD) {
        groups.push({
          ids: [first.id, second.id].sort(),
          similarity
        });
      }
    }
  }
  return groups;
}

function setsEqual(first, second) {
  return first.size === second.size &&
    [...first].every((value) => second.has(value));
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
