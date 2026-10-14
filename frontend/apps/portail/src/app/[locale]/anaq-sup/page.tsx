import { InstitutionalPageView, institutionalMetadata } from "@/components/content/InstitutionalPage";

export const revalidate = 600;

export const generateMetadata = () => institutionalMetadata("presentation");

export default function Page() {
  return <InstitutionalPageView slug="presentation" />;
}
