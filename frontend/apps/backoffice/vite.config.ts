import { fileURLToPath } from "node:url";

import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Chemins relatifs : la même image sert sous n'importe quelle adresse d'accès (<base href> posé au démarrage).
  base: "./",
  resolve: {
    alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
  },
  build: {
    sourcemap: false,
    // Fichiers hachés → cache immuable côté Nginx (voir nginx.conf.template).
    assetsDir: "assets",
  },
  server: {
    // En local, l'API Spring Boot tourne sur :8080.
    proxy: { "/api": "http://localhost:8080" },
  },
});
