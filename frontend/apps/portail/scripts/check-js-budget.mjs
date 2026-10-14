// Budget de performance : JavaScript initial, gzip, navigateurs modernes.
// Les pages sont rendues à la requête : le script lit le HTML d'un serveur démarré sur le build
// (`node .next/standalone/apps/portail/server.js`) et mesure les chunks dans .next/static.
// Sans backend (CI), le script sert une API factice aux listes vides sur BUDGET_STUB_API_PORT :
// démarrer le portail avec API_INTERNAL_URL=http://127.0.0.1:<ce port>, les pages s'affichent alors
// normalement (sans API, une liste répondrait « service indisponible » et ne serait pas mesurable).
//   BUDGET_BASE_URL=http://127.0.0.1:3000 node scripts/check-js-budget.mjs
import { readFileSync } from "node:fs";
import { createServer } from "node:http";
import { join } from "node:path";
import { gzipSync } from "node:zlib";

const STUB_PORT = process.env.BUDGET_STUB_API_PORT;
if (STUB_PORT) {
  const emptyPage = { content: [], page: { page: 0, size: 12, totalElements: 0, totalPages: 0 } };
  createServer((request, response) => {
    const path = new URL(request.url ?? "/", "http://stub").pathname.replace(/^\/api\/v1\/public/, "");
    const body = /\/(categories|years)$|^\/partners$/.test(path) ? []
      : /^\/(news|events|publications|establishments|programs)$/.test(path) ? emptyPage
      : null; // fiches et pages : introuvables
    response.writeHead(body ? 200 : 404, { "content-type": "application/json" });
    response.end(JSON.stringify(body ?? { title: "Introuvable" }));
  }).listen(Number(STUB_PORT), "127.0.0.1").unref();
}

const BASE_URL = process.env.BUDGET_BASE_URL ?? "http://127.0.0.1:3000";
const BUDGETS_KB = {
  // Accueil : animations Motion (LazyMotion + m) arbitrées avec l'ANAQ-Sup.
  "/": 160,
  "/habilitations-et-accreditations": 160,
  // Pages de contenu sans animation JS.
  "/procedures/habilitation": 150,
  // Documents : seul le petit formulaire de filtres (application immédiate des listes) est côté client.
  "/ressources-documentaires/publications": 150,
  // Version anglaise : mêmes composants, aucun code de traduction envoyé au navigateur.
  "/en": 160,
  "/en/news": 150,
};

const nextDir = new URL("../.next/", import.meta.url).pathname;

let failed = false;
for (const [path, budget] of Object.entries(BUDGETS_KB)) {
  const response = await fetch(BASE_URL + path).catch(() => null);
  if (!response) {
    console.error(`✗ ${path} : serveur injoignable sur ${BASE_URL} (démarrer le build standalone d'abord)`);
    process.exit(2);
  }
  if (!response.ok) {
    console.error(`✗ ${path} : réponse ${response.status} (API injoignable ? voir BUDGET_STUB_API_PORT)`);
    process.exit(2);
  }
  const html = await response.text();
  const scripts = new Set();
  for (const [, tag, src] of html.matchAll(/(<script[^>]*src="\/_next\/(static\/[^"]+\.js)"[^>]*>)/g)) {
    // Les scripts nomodule (polyfills) ne sont pas téléchargés par les navigateurs modernes.
    if (!/nomodule/i.test(tag)) scripts.add(src);
  }
  const totalKb = [...scripts]
    .map((src) => gzipSync(readFileSync(join(nextDir, src)), { level: 9 }).length)
    .reduce((sum, size) => sum + size, 0) / 1024;
  const ok = totalKb <= budget;
  failed ||= !ok;
  console.log(`${ok ? "✓" : "✗"} ${path} : ${totalKb.toFixed(1)} Ko gzip (budget ${budget} Ko)`);
}
process.exit(failed ? 1 : 0);
