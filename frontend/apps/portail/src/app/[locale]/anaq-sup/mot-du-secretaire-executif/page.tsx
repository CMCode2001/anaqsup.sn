import { InstitutionalPageView, institutionalMetadata } from "@/components/content/InstitutionalPage";

export const revalidate = 600;

export const generateMetadata = () => institutionalMetadata("mot-du-secretaire-executif");

export default function Page() {
  return <InstitutionalPageView slug="mot-du-secretaire-executif" />;
}
