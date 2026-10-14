import { notFound } from "next/navigation";

/** Toute adresse inconnue d'une langue affiche la page 404 traduite, dans la mise en page du site. */
export default function CatchAll() {
  notFound();
}
