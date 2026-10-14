import { serverEnv } from "@/lib/env";

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

/**
 * Images et documents PDF téléversés dans le back-office (photo du Secrétaire exécutif, logos, ressources
 * documentaires…), servis sur l'adresse du site (/media/<clé>) : même origine pour le navigateur et pour
 * l'optimiseur d'images de Next.js.
 * Une clé ne change jamais de contenu : cache d'un an, transmis tel quel par le backend.
 */
export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!UUID.test(id)) return new Response(null, { status: 404 });

  const upstream = await fetch(`${serverEnv.apiInternalUrl}/api/v1/public/media/${id}`, { cache: "no-store" });
  if (!upstream.ok || !upstream.body) return new Response(null, { status: upstream.status === 404 ? 404 : 502 });

  const headers = new Headers();
  // content-disposition : un PDF s'ouvre dans le navigateur (« Consulter ») et se télécharge sous son nom d'origine.
  for (const name of ["content-type", "content-length", "content-disposition", "cache-control", "etag"]) {
    const value = upstream.headers.get(name);
    if (value) headers.set(name, value);
  }
  headers.set("x-content-type-options", "nosniff");
  return new Response(upstream.body, { status: 200, headers });
}
