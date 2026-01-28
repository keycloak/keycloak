import { FormSubmitButton } from "@keycloak/keycloak-ui-shared";
import { ActionGroup, Button, PageSection } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { FormAccess } from "../components/form/FormAccess";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  OrganizationForm,
  OrganizationFormType,
  convertToOrg,
} from "./OrganizationForm";
import { toEditOrganization } from "./routes/EditOrganization";
import { toOrganizations } from "./routes/Organizations";

export default function NewOrganization() {
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { realm } = useRealm();
  const form = useForm({ mode: "onChange" });
  const { handleSubmit, formState } = form;

  const save = async (org: OrganizationFormType) => {
    try {
      const organization = convertToOrg(org);
      const { id } = await adminClient.organizations.create(organization);
      addAlert(t("organizationSaveSuccess"));
      navigate(toEditOrganization({ realm, id, tab: "settings" }));
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
