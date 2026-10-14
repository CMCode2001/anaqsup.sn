import { ProcedurePage, procedureMetadata } from "@/components/content/ProcedurePage";

export const generateMetadata = () => procedureMetadata("habilitation");

export default function Page() {
  return <ProcedurePage procedure="habilitation" />;
}
