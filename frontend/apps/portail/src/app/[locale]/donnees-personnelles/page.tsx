import { LegalPageView, legalMetadata } from "@/components/content/LegalPage";

export const revalidate = 600;

export const generateMetadata = () => legalMetadata("donnees-personnelles");

export default function Page() {
  return <LegalPageView slug="donnees-personnelles" />;
}
