export const dynamic = "force-dynamic";

/** Sonde de vie du conteneur (healthcheck Docker), indépendante de l'API. */
export function GET() {
  return Response.json({ status: "UP" });
}
