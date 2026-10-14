import { type AdminProblem, type AdminUploaded, type AdminUploadedDocument, createAdminClient } from "@anaqsup/api-client";

/**
 * Adresse d'accès du back-office, ex. "/espace-…/". Elle n'est pas publiée : Nginx l'injecte dans
 * <base href> au démarrage, et tout le reste (routes, API, images) s'en déduit.
 */
export const BASE_PATH = new URL(document.baseURI).pathname;

/** Basename du routeur : "/espace-…" (sans barre finale), ou "" en développement local. */
export const ROUTER_BASENAME = BASE_PATH.replace(/\/$/, "");

export const api = createAdminClient(`${BASE_PATH}api/v1`);

/** Erreur d'API au format RFC 9457 : `detail` est déjà rédigé en français par le backend. */
export class ApiError extends Error {
  constructor(readonly status: number, readonly problem?: AdminProblem) {
    super(problem?.detail ?? problem?.title ?? `Erreur ${status}`);
  }

  get code() {
    return this.problem?.code;
  }
}

/** Renvoie les données d'un appel openapi-fetch, ou lève une ApiError. */
export async function unwrap<T>(call: Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T> {
  const { data, error, response } = await call;
  if (!response.ok) throw new ApiError(response.status, error as AdminProblem | undefined);
  return data as T;
}

/** Mot de passe provisoire lisible (sans caractères ambigus), 12 caractères. */
export function generatePassword(length = 12): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
  const values = crypto.getRandomValues(new Uint32Array(length));
  return Array.from(values, (value) => alphabet[value % alphabet.length]).join("");
}

export const dateTime = new Intl.DateTimeFormat("fr-SN", { dateStyle: "medium", timeStyle: "short", timeZone: "Africa/Dakar" });

function cookie(name: string): string | undefined {
  return document.cookie.split("; ").find((entry) => entry.startsWith(`${name}=`))?.slice(name.length + 1);
}

/** Envoi multipart (openapi-fetch ne gère pas les fichiers) avec le jeton CSRF. */
async function postFile<T>(path: string, file: File): Promise<T> {
  const body = new FormData();
  body.append("file", file);
  const token = cookie("XSRF-TOKEN");
  const response = await fetch(`${BASE_PATH}api/v1${path}`, {
    method: "POST",
    body,
    credentials: "same-origin",
    headers: token ? { "X-XSRF-TOKEN": decodeURIComponent(token) } : {},
  });
  const data = await response.json().catch(() => undefined);
  if (!response.ok) throw new ApiError(response.status, data as AdminProblem | undefined);
  return data as T;
}

/** Téléverse une image (PNG, JPEG ou WebP, 5 Mo maximum) ; renvoie la clé à enregistrer dans le contenu. */
export function uploadImage(file: File): Promise<AdminUploaded> {
  return postFile("/admin/media", file);
}

/** Téléverse un PDF (30 Mo maximum) ; le serveur renvoie aussi sa vignette (première page). */
export function uploadDocument(file: File): Promise<AdminUploadedDocument> {
  return postFile("/admin/media/documents", file);
}

/** « 1,2 Mo », « 864 ko ». */
export function fileSize(bytes: number | null | undefined): string {
  if (bytes == null) return "";
  const kb = bytes / 1024;
  return kb < 1024
    ? `${new Intl.NumberFormat("fr-SN", { maximumFractionDigits: 0 }).format(Math.max(1, kb))} ko`
    : `${new Intl.NumberFormat("fr-SN", { maximumFractionDigits: 1 }).format(kb / 1024)} Mo`;
}

/**
 * Aperçu d'une image dans le back-office. L'adresse publique « /media/<clé> » est celle du site ;
 * ici, la même image est lue par l'API (même origine que le back-office).
 */
export function previewUrl(url: string | null | undefined): string | undefined {
  if (!url) return undefined;
  const media = /^\/media\/([0-9a-f-]{36})$/.exec(url);
  return media ? `${BASE_PATH}api/v1/public/media/${media[1]}` : undefined;
}
