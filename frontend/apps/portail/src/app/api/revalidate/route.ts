import { createHmac, timingSafeEqual } from "node:crypto";

import { revalidateTag } from "next/cache";

import { serverEnv } from "@/lib/env";

const TAG = /^[a-z]+(:[a-z0-9-]+)?$/;
const MAX_SKEW_SECONDS = 300;

/**
 * Webhook appelé par le backend quand un contenu est modifié dans le back-office.
 * Signature : `X-Anaqsup-Signature: sha256=<hex>` = HMAC-SHA256(secret, `${timestamp}.${body}`),
 * avec `X-Anaqsup-Timestamp` (secondes epoch) pour empêcher le rejeu.
 * Corps : `{ "tags": ["page:mot-du-secretaire-executif", "partners"] }`.
 */
export async function POST(request: Request) {
  const body = await request.text();
  if (!isAuthentic(request.headers, body)) {
    return Response.json({ title: "Signature invalide" }, { status: 401 });
  }

  let tags: unknown;
  try {
    tags = (JSON.parse(body) as { tags?: unknown }).tags;
  } catch {
    return Response.json({ title: "Corps JSON invalide" }, { status: 400 });
  }
  if (!Array.isArray(tags) || tags.length === 0 || tags.length > 50 || !tags.every((t) => typeof t === "string" && TAG.test(t))) {
    return Response.json({ title: "Liste de tags invalide" }, { status: 400 });
  }

  // Modification faite dans le back-office : l'ancienne version n'est plus jamais servie, la prochaine
  // visite recharge la donnée (l'éditeur voit aussitôt son changement en ligne).
  for (const tag of tags as string[]) revalidateTag(tag, { expire: 0 });
  return Response.json({ revalidated: tags.length });
}

function isAuthentic(headers: Headers, body: string): boolean {
  const secret = serverEnv.revalidateSecret;
  const timestamp = headers.get("x-anaqsup-timestamp");
  const signature = headers.get("x-anaqsup-signature")?.replace(/^sha256=/, "");
  if (secret.length < 32 || !timestamp || !signature) return false;
  if (Math.abs(Date.now() / 1000 - Number(timestamp)) > MAX_SKEW_SECONDS) return false;

  const expected = createHmac("sha256", secret).update(`${timestamp}.${body}`).digest();
  const received = Buffer.from(signature, "hex");
  return received.length === expected.length && timingSafeEqual(received, expected);
}
