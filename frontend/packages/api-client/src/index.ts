import createClient from "openapi-fetch";

import type { components as adminComponents, paths as adminPaths } from "./admin-schema";
import type { components, paths } from "./schema";

export type { components, paths };

type Schemas = components["schemas"];

// Alias lisibles des schémas du contrat (source : contracts/openapi/public-api.v1.yaml).
export type Avis = Schemas["Avis"];
export type Lang = Schemas["Lang"];
export type EstablishmentSummary = Schemas["EstablishmentSummary"];
export type Establishment = Schemas["Establishment"];
export type EstablishmentPage = Schemas["EstablishmentPage"];
export type ProgramSummary = Schemas["ProgramSummary"];
export type ProgramPage = Schemas["ProgramPage"];
export type ProgramLevel = Schemas["ProgramLevel"];
export type NewsPage = Schemas["NewsPage"];
export type NewsItem = Schemas["NewsItem"];
export type NewsCategory = Schemas["NewsCategory"];
export type EventPage = Schemas["EventPage"];
export type Event = Schemas["Event"];
export type PublicationPage = Schemas["PublicationPage"];
export type PublicationKind = Schemas["PublicationKind"];
export type InstitutionalPage = Schemas["InstitutionalPage"];
export type Partner = Schemas["Partner"];
export type Statistics = Schemas["Statistics"];
export type SiteSettings = Schemas["SiteSettings"];
export type Problem = Schemas["Problem"];

/**
 * Client typé de bout en bout (chemins, paramètres, réponses), pour les usages côté navigateur
 * (back-office, autocomplétion). Le portail utilise son propre `fetch` serveur pour profiter
 * du cache ISR de Next.js (voir apps/portail/src/lib/api.ts).
 */
export function createApiClient(baseUrl: string, init?: { headers?: Record<string, string> }) {
  return createClient<paths>({ baseUrl: `${baseUrl.replace(/\/$/, "")}/api/v1`, headers: init?.headers });
}

// ----------------------------------------------------------------------------- API d'administration

type AdminSchemas = adminComponents["schemas"];

export type { adminPaths };
export type AdminMe = AdminSchemas["Me"];
export type AdminAccount = AdminSchemas["Account"];
export type AdminRole = AdminSchemas["Role"];
export type AdminPermission = AdminSchemas["Permission"];
export type AdminProblem = AdminSchemas["Problem"];
export type AdminPage = AdminSchemas["Page"];
export type AdminPageSummary = AdminSchemas["PageSummary"];
export type AdminPartner = AdminSchemas["Partner"];
export type AdminUploaded = AdminSchemas["Uploaded"];
export type AdminUploadedDocument = AdminSchemas["UploadedDocument"];
export type AdminPublication = AdminSchemas["Publication"];
export type AdminPublicationRequest = AdminSchemas["PublicationRequest"];
export type AdminPublicationKind = AdminSchemas["PublicationKind"];
export type AdminNews = AdminSchemas["News"];
export type AdminNewsRequest = AdminSchemas["NewsRequest"];
export type AdminNewsCategory = AdminSchemas["NewsCategory"];
export type AdminEvent = AdminSchemas["Event"];
export type AdminEventRequest = AdminSchemas["EventRequest"];
export type AdminEstablishment = AdminSchemas["Establishment"];
export type AdminEstablishmentRequest = AdminSchemas["EstablishmentRequest"];
export type AdminProgram = AdminSchemas["Program"];
export type AdminProgramRequest = AdminSchemas["ProgramRequest"];
export type AdminProgramLevel = AdminSchemas["ProgramLevel"];
export type AdminAvis = AdminSchemas["Avis"];
export type AdminSiteSettings = AdminSchemas["SiteSettings"];
export type AdminSettings = AdminSchemas["AdminSettings"];

function cookie(name: string): string | undefined {
  return document.cookie.split("; ").find((entry) => entry.startsWith(`${name}=`))?.slice(name.length + 1);
}

/**
 * Client du back-office (même origine) : cookie de session envoyé, et jeton CSRF (cookie `XSRF-TOKEN`)
 * recopié dans l'en-tête `X-XSRF-TOKEN` de toute requête qui modifie quelque chose.
 * `apiBase` : préfixe d'accès de l'API, ex. "/espace-…/api/v1".
 */
export function createAdminClient(apiBase: string) {
  const client = createClient<adminPaths>({ baseUrl: apiBase.replace(/\/$/, ""), credentials: "same-origin" });
  client.use({
    onRequest({ request }) {
      if (request.method !== "GET" && request.method !== "HEAD") {
        const token = cookie("XSRF-TOKEN");
        if (token) request.headers.set("X-XSRF-TOKEN", decodeURIComponent(token));
      }
      return request;
    },
  });
  return client;
}
