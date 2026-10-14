import "server-only";

function required(name: string, fallback?: string): string {
  const value = process.env[name] ?? fallback;
  if (!value) {
    throw new Error(`Variable d'environnement manquante : ${name}`);
  }
  return value;
}

/** Configuration serveur, lue une fois. Ne jamais importer depuis un Client Component. */
export const serverEnv = {
  apiInternalUrl: required("API_INTERNAL_URL", "http://localhost:8080").replace(/\/$/, ""),
  revalidateSecret: process.env.REVALIDATE_SECRET ?? "",
};
