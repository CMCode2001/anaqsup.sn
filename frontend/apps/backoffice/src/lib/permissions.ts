import type { AdminPermission } from "@anaqsup/api-client";

/** Libellés des permissions, dans l'ordre d'affichage. */
export const PERMISSIONS: { value: AdminPermission; label: string; description: string }[] = [
  { value: "ACTUALITES", label: "Actualités", description: "Articles, catégories et article « À la une »" },
  { value: "EVENEMENTS", label: "Événements", description: "Ateliers, séminaires, colloques" },
  { value: "DOCUMENTS", label: "Ressources documentaires", description: "Publications, textes réglementaires, référentiels, guides" },
  { value: "PAGES", label: "Pages institutionnelles", description: "Présentation, missions, gouvernance, organigramme, mot du SE" },
  { value: "AVIS", label: "Habilitations & accréditations", description: "Avis rendus sur les établissements et les formations" },
  { value: "PARTENAIRES", label: "Partenaires", description: "Logos et liens du bandeau de l'accueil" },
  { value: "PARAMETRES", label: "Paramètres du site", description: "Coordonnées, réseaux sociaux, liens utiles, textes d'introduction" },
  { value: "COMPTES", label: "Comptes et rôles", description: "Créer des comptes, attribuer des rôles" },
];

export const permissionLabel = (value: AdminPermission) => PERMISSIONS.find((p) => p.value === value)?.label ?? value;
