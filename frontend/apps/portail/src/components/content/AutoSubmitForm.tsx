"use client";

import { useRouter } from "next/navigation";
import type { ComponentProps } from "react";

/**
 * Formulaire de filtres en GET : sans JavaScript, il s'envoie normalement (bouton « Rechercher ») ; avec,
 * une liste déroulante s'applique dès qu'on la change et la page se met à jour sans rechargement.
 * Les champs vides sont omis de l'adresse ; « goto » (numéro de page saisi, à partir de 1) devient « page ».
 */
export function AutoSubmitForm({ action, children, ...props }: Omit<ComponentProps<"form">, "action" | "method" | "onSubmit" | "onChange"> & {
  action: string;
}) {
  const router = useRouter();

  const navigate = (form: HTMLFormElement) => {
    const params = new URLSearchParams();
    for (const [key, raw] of new FormData(form)) {
      const value = typeof raw === "string" ? raw.trim() : "";
      if (!value) continue;
      if (key === "goto") {
        const page = Number.parseInt(value, 10);
        if (page > 1) params.set("page", String(page - 1));
      } else {
        params.set(key, value);
      }
    }
    const query = params.toString();
    router.push(query ? `${action}?${query}` : action, { scroll: false });
  };

  return (
    <form
      {...props}
      action={action}
      method="get"
      onSubmit={(event) => {
        event.preventDefault();
        navigate(event.currentTarget);
      }}
      onChange={(event) => {
        if (event.target instanceof HTMLSelectElement) navigate(event.currentTarget);
      }}
    >
      {children}
    </form>
  );
}
