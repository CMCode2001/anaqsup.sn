// Import de types uniquement (aucun code) : charge les déclarations de next-intl pour pouvoir les étendre.
import type {} from "next-intl";

import type messages from "../messages/fr.json";
import type { routing } from "./i18n/routing";

// Clés de traduction et langues vérifiées à la compilation (t("Nav.inexistant") ne compile pas).
declare module "next-intl" {
  interface AppConfig {
    Locale: (typeof routing.locales)[number];
    Messages: typeof messages;
  }
}
