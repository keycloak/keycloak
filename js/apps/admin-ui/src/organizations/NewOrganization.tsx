import { PageSection } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { OrganizationForm } from "./OrganizationForm";

export default function NewOrganization() {
  const form = useForm();

  const save = (org: any) => {
    console.log(org);
  };

  return (
    <>
      <ViewHeader titleKey="createOrganization" />
      <PageSection variant="light">
        <FormProvider {...form}>
          <OrganizationForm save={save} />
        </FormProvider>
      </PageSection>
    </>
  );
}
