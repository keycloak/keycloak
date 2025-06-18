import { AlertVariant, PageSection } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  LdapComponentRepresentation,
  UserFederationLdapForm,
  serializeFormData,
} from "./UserFederationLdapForm";
import { toUserFederation } from "./routes/UserFederation";
import { ExtendedHeader } from "./shared/ExtendedHeader";

export default function CreateUserFederationLdapSettings() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<LdapComponentRepresentation>({ mode: "onChange" });
  const navigate = useNavigate();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();

  const onSubmit = async (formData: LdapComponentRepresentation) => {
    try {
      await adminClient.components.create(serializeFormData(formData));
      addAlert(t("createUserProviderSuccess"), AlertVariant.success);
      navigate(toUserFederation({ realm }));
    } catch (error) {
      addError("createUserProviderError", error);
    }
  };

  return (
    <FormProvider {...form}>
      <ExtendedHeader
        provider="LDAP"
        noDivider
        save={() => form.handleSubmit(onSubmit)()}
      />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <PageSection variant="light">
          <UserFederationLdapForm onSubmit={onSubmit} />
        </PageSection>
      </PageSection>
    </FormProvider>
  );
}
