import { InstitutionalPageView, institutionalMetadata } from "@/components/content/InstitutionalPage";

export const revalidate = 600;

export const generateMetadata = () => institutionalMetadata("gouvernance");

export default function Page() {
  return <InstitutionalPageView slug="gouvernance" />;
}
