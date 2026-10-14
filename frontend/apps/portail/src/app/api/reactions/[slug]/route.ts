import { randomUUID } from "node:crypto";

import { cookies } from "next/headers";
import type { NextRequest } from "next/server";

import { reactionsApi, type Reactions, SLUG, VISITOR, VISITOR_COOKIE, type Vote } from "@/lib/reactions";

const VOTES: readonly Vote[] = ["like", "dislike", "none"];
const ONE_YEAR = 60 * 60 * 24 * 365;
const NO_STORE = { "Cache-Control": "no-store" };

/**
 * Mentions « J'aime » / « Je n'aime pas » d'une actualité, pour le navigateur du lecteur (même origine).
 * Le navigateur est reconnu par un identifiant anonyme tiré au hasard, posé dans un cookie au premier vote
 * seulement : aucun cookie pour qui ne vote pas. Débit limité par adresse IP dans Nginx.
 */
export async function GET(_request: NextRequest, ctx: RouteContext<"/api/reactions/[slug]">) {
  const { slug } = await ctx.params;
  if (!SLUG.test(slug)) return problem(404, "Actualité introuvable");
  return relay(await reactionsApi("GET", slug, await visitor()));
}

export async function PUT(request: NextRequest, ctx: RouteContext<"/api/reactions/[slug]">) {
  const { slug } = await ctx.params;
  if (!SLUG.test(slug)) return problem(404, "Actualité introuvable");
  // Vote venu d'un autre site (formulaire ou script tiers) : refusé.
  if (!sameOrigin(request)) return problem(403, "Origine refusée");
  if (!request.headers.get("content-type")?.startsWith("application/json")) return problem(415, "JSON attendu");

  let vote: unknown;
  try {
    vote = ((await request.json()) as { reaction?: unknown }).reaction;
  } catch {
    return problem(400, "Corps JSON invalide");
  }
  if (typeof vote !== "string" || !VOTES.includes(vote as Vote)) return problem(400, "Vote invalide");

  const known = await visitor();
  const id = known ?? randomUUID();
  const response = await reactionsApi("PUT", slug, id, vote as Vote);
  if (response.ok) {
    (await cookies()).set(VISITOR_COOKIE, id, {
      httpOnly: true,
      sameSite: "lax",
      secure: isHttps(request),
      path: "/api/reactions",
      maxAge: ONE_YEAR,
    });
  }
  return relay(response);
}

async function visitor(): Promise<string | null> {
  const value = (await cookies()).get(VISITOR_COOKIE)?.value;
  return value && VISITOR.test(value) ? value : null;
}

/** Réponse de l'API transmise telle quelle (nombre de « J'aime » et vote du navigateur), jamais mise en cache. */
async function relay(response: Response) {
  if (response.status === 404) return problem(404, "Actualité introuvable");
  if (!response.ok) return problem(502, "Service momentanément indisponible");
  const { likes, mine } = (await response.json()) as Reactions;
  return Response.json({ likes, mine: mine ?? null } satisfies Reactions, { headers: NO_STORE });
}

function sameOrigin(request: NextRequest): boolean {
  const site = request.headers.get("sec-fetch-site");
  if (site) return site === "same-origin";
  const origin = request.headers.get("origin");
  return origin === null || origin === new URL(request.url).origin || origin === `${isHttps(request) ? "https" : "http"}://${request.headers.get("host")}`;
}

function isHttps(request: NextRequest): boolean {
  return request.headers.get("x-forwarded-proto") === "https" || request.nextUrl.protocol === "https:";
}

function problem(status: number, title: string) {
  return Response.json({ title, status }, { status, headers: { ...NO_STORE, "Content-Type": "application/problem+json" } });
}
