import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, Navigate, RouterProvider } from "react-router";

import "@anaqsup/ui/globals.css";
import "@fontsource/poppins/400.css";
import "@fontsource/poppins/500.css";
import "@fontsource/poppins/600.css";
import "@fontsource/poppins/700.css";
import "./index.css";
import { Toaster } from "@anaqsup/ui/components/sonner";
import { TooltipProvider } from "@anaqsup/ui/components/tooltip";

import { Layout } from "./Layout";
import { ROUTER_BASENAME } from "./lib/api";
import { AuthGate } from "./lib/auth";
import { AccountsPage } from "./pages/AccountsPage";
import { Dashboard } from "./pages/Dashboard";
import { DocumentsPage } from "./pages/DocumentsPage";
import { EventsPage } from "./pages/EventsPage";
import { NewsPage } from "./pages/NewsPage";
import { PagesPage } from "./pages/PagesPage";
import { PartnersPage } from "./pages/PartnersPage";
import { ProfilePage } from "./pages/ProfilePage";
import { RegistryPage } from "./pages/RegistryPage";
import { RolesPage } from "./pages/RolesPage";

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, retry: 1 }, mutations: { retry: false } },
});

// Le serveur refuse de toute façon ce que le rôle n'autorise pas (403) ; le menu masque ces rubriques.
const router = createBrowserRouter(
  [
    {
      path: "/",
      element: <AuthGate><Layout /></AuthGate>,
      children: [
        { index: true, element: <Dashboard /> },
        { path: "actualites", element: <NewsPage /> },
        // Éditeurs de texte (Tiptap) chargés seulement à l'ouverture d'un contenu : ils n'alourdissent pas le reste.
        { path: "actualites/:id", lazy: async () => ({ Component: (await import("./pages/NewsEditorPage")).NewsEditorPage }) },
        { path: "evenements", element: <EventsPage /> },
        { path: "evenements/:id", lazy: async () => ({ Component: (await import("./pages/EventEditorPage")).EventEditorPage }) },
        { path: "ressources", element: <DocumentsPage /> },
        { path: "pages", element: <PagesPage /> },
        { path: "pages/:slug", lazy: async () => ({ Component: (await import("./pages/PageEditorPage")).PageEditorPage }) },
        { path: "partenaires", element: <PartnersPage /> },
        { path: "avis", element: <RegistryPage /> },
        { path: "comptes", element: <AccountsPage /> },
        { path: "roles", element: <RolesPage /> },
        // Textes d'origine du portail (messages) chargés avec cet écran seulement.
        { path: "parametres", lazy: async () => ({ Component: (await import("./pages/SettingsPage")).SettingsPage }) },
        { path: "profil", element: <ProfilePage /> },
        { path: "*", element: <Navigate to="/" replace /> },
      ],
    },
  ],
  { basename: ROUTER_BASENAME || "/" },
);

const root = document.getElementById("root");
if (!root) throw new Error("#root introuvable");

createRoot(root).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <RouterProvider router={router} />
        <Toaster richColors position="top-right" />
      </TooltipProvider>
    </QueryClientProvider>
  </StrictMode>,
);
