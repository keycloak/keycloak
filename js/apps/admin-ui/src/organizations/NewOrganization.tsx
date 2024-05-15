import { FormSubmitButton } from "@keycloak/keycloak-ui-shared";
import { ActionGroup, Button, PageSection } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { FormAccess } from "../components/form/FormAccess";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  OrganizationForm,
  OrganizationFormType,
  convertToOrg,
} from "./OrganizationForm";
import { toOrganizations } from "./routes/Organizations";

export default function NewOrganization() {
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();
  const { realm } = useRealm();
  const form = useForm();
  const { handleSubmit, formState } = form;

  const save = async (org: OrganizationFormType) => {
    try {
      const organization = convertToOrg(org);
      await adminClient.organizations.create(organization);
      addAlert("organizationSaveSuccess");
    } catch (error) {
      addError("organizationSaveError", error);
    }
  };

  return (
    <>
      <ViewHeader titleKey="createOrganization" />
      <PageSection variant="light">
        <FormAccess role="anyone" onSubmit={handleSubmit(save)} isHorizontal>
          <FormProvider {...form}>
            <OrganizationForm />
            <ActionGroup>
              <FormSubmitButton formState={formState} data-testid="save">
                {t("save")}
              </FormSubmitButton>
              <Button
                data-testid="cancel"
                variant="link"
                component={(props) => (
                  <Link {...props} to={toOrganizations({ realm })} />
                )}
              >
                {t("cancel")}
              </Button>
            </ActionGroup>
          </FormProvider>
        </FormAccess>
      </PageSection>
    </>
  );
}
