import { InstitutionalPageView, institutionalMetadata } from "@/components/content/InstitutionalPage";

export const revalidate = 600;

export const generateMetadata = () => institutionalMetadata("missions");

export default function Page() {
  return <InstitutionalPageView slug="missions" />;
}
