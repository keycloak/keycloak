import React from "react";
import { useHistory, useRouteMatch } from "react-router-dom";
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
import { OIDCGeneralSettings } from "./OIDCGeneralSettings";
import { OpenIdConnectSettings } from "./OpenIdConnectSettings";
import { useRealm } from "../../context/realm-context/RealmContext";
import { OIDCAuthentication } from "./OIDCAuthentication";
import { useAlerts } from "../../components/alert/Alerts";

type DiscoveryIdentity = IdentityProviderRepresentation & {
  discoveryEndpoint?: string;
};

export const AddOpenIdConnect = () => {
  const { t } = useTranslation("identity-providers");
  const history = useHistory();
  const { url } = useRouteMatch();
  const isKeycloak = url.endsWith("keycloak-oidc");
  const id = `${isKeycloak ? "keycloak-" : ""}oidc`;

  const form = useForm<IdentityProviderRepresentation>({
    defaultValues: { alias: id },
  });
  const {
    handleSubmit,
    formState: { isDirty },
  } = form;

  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const save = async (provider: DiscoveryIdentity) => {
    delete provider.discoveryEndpoint;
    try {
      await adminClient.identityProviders.create({
        ...provider,
        providerId: id,
      });
      addAlert(t("createSuccess"), AlertVariant.success);
      history.push(`/${realm}/identity-providers/${id}/settings`);
    } catch (error) {
      addError("identity-providers:createError", error);
    }
  };

  return (
    <>
      <ViewHeader
        titleKey={t(
          isKeycloak ? "addKeycloakOpenIdProvider" : "addOpenIdProvider"
        )}
      />
      <PageSection variant="light">
        <FormProvider {...form}>
          <FormAccess
            role="manage-identity-providers"
            isHorizontal
            onSubmit={handleSubmit(save)}
          >
            <OIDCGeneralSettings id={id} />
            <OpenIdConnectSettings />
            <OIDCAuthentication />
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
                onClick={() => history.push(`/${realm}/identity-providers`)}
              >
                {t("common:cancel")}
              </Button>
            </ActionGroup>
          </FormAccess>
        </FormProvider>
      </PageSection>
    </>
  );
};
