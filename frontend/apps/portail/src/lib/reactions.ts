import "server-only";

import { createHmac } from "node:crypto";

import { serverEnv } from "./env";

/** Vote d'un navigateur : seul le nombre de « J'aime » est public (décision ANAQ-Sup du 14/10/2026). */
export type Reaction = "LIKE" | "DISLIKE";
export type Reactions = { likes: number; mine: Reaction | null };
export type Vote = "like" | "dislike" | "none";

export const VISITOR_COOKIE = "anaqsup_lecteur";
export const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;
export const VISITOR = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

/**
 * Appel du portail vers l'API pour les mentions « J'aime » : chemin interne `/api/v1/portal`, jamais exposé par
 * Nginx, signé avec le secret partagé (REVALIDATE_SECRET), comme le webhook de revalidation dans l'autre sens.
 * Signature : HMAC-SHA256(secret, "timestamp.MÉTHODE.chemin.visiteur") — voir PortalSignature côté API.
 */
export async function reactionsApi(method: "GET" | "PUT", slug: string, visitor: string | null, vote?: Vote): Promise<Response> {
  const path = `/api/v1/portal/news/${slug}/reactions${vote ? `/${vote}` : ""}`;
  const timestamp = String(Math.floor(Date.now() / 1000));
  const signature = createHmac("sha256", serverEnv.revalidateSecret)
    .update(`${timestamp}.${method}.${path}.${visitor ?? ""}`)
    .digest("hex");
  const headers: Record<string, string> = {
    Accept: "application/json",
    "X-Anaqsup-Timestamp": timestamp,
    "X-Anaqsup-Signature": `sha256=${signature}`,
  };
  if (visitor) headers["X-Anaqsup-Visitor"] = visitor;
  return fetch(`${serverEnv.apiInternalUrl}${path}`, { method, headers, cache: "no-store" });
}
