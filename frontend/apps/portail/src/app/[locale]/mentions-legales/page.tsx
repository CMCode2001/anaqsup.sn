import { LegalPageView, legalMetadata } from "@/components/content/LegalPage";

export const revalidate = 600;

export const generateMetadata = () => legalMetadata("mentions-legales");

export default function Page() {
  return <LegalPageView slug="mentions-legales" />;
}
