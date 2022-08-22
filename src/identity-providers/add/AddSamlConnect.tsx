import { Link } from "react-router-dom-v5-compat";
import { useNavigate } from "react-router-dom-v5-compat";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";

import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useAdminClient } from "../../context/auth/AdminClient";
import { SamlGeneralSettings } from "./SamlGeneralSettings";
import { SamlConnectSettings } from "./SamlConnectSettings";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";
import { toIdentityProvider } from "../routes/IdentityProvider";
import { toIdentityProviders } from "../routes/IdentityProviders";

type DiscoveryIdentityProvider = IdentityProviderRepresentation & {
  discoveryEndpoint?: string;
};

export default function AddSamlConnect() {
  const { t } = useTranslation("identity-providers");
  const navigate = useNavigate();
  const id = "saml";

  const form = useForm<DiscoveryIdentityProvider>({
    defaultValues: { alias: id, config: { allowCreate: "true" } },
  });
  const {
    handleSubmit,
    formState: { isDirty },
  } = form;

  const { adminClient } = useAdminClient();
  const { addAlert } = useAlerts();
  const { realm } = useRealm();

  const save = async (provider: DiscoveryIdentityProvider) => {
    delete provider.discoveryEndpoint;
    try {
      await adminClient.identityProviders.create({
        ...provider,
        providerId: id,
      });
      addAlert(t("createSuccess"), AlertVariant.success);
      navigate(
        toIdentityProvider({
          realm,
          providerId: id,
          alias: provider.alias!,
          tab: "settings",
        })
      );
    } catch (error: any) {
      addAlert(
        t("createError", {
          error: error.response?.data?.errorMessage || error,
        }),
        AlertVariant.danger
      );
    }
  };

  return (
    <>
      <ViewHeader titleKey={t("addSamlProvider")} />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            role="manage-identity-providers"
            isHorizontal
            onSubmit={handleSubmit(save)}
          >
            <SamlGeneralSettings id={id} />
            <SamlConnectSettings />
            <ActionGroup>
              <Button
                isDisabled={!isDirty}
                variant="primary"
                type="submit"
                data-testid="createProvider"
              >
                {t("common:add")}
              </Button>
              <Button
                variant="link"
                data-testid="cancel"
                component={(props) => (
                  <Link {...props} to={toIdentityProviders({ realm })} />
                )}
              >
                {t("common:cancel")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
}
