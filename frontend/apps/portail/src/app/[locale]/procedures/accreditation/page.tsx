import { ProcedurePage, procedureMetadata } from "@/components/content/ProcedurePage";

export const generateMetadata = () => procedureMetadata("accreditation");

export default function Page() {
  return <ProcedurePage procedure="accreditation" />;
}
