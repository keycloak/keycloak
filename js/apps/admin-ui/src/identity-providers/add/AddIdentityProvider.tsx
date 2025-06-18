import type IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";
import { useMemo } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../components/form/FormAccess";
import { ViewHeader } from "../../components/view-header/ViewHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { toUpperCase } from "../../util";
import { useParams } from "../../utils/useParams";
import { toIdentityProvider } from "../routes/IdentityProvider";
import type { IdentityProviderCreateParams } from "../routes/IdentityProviderCreate";
import { toIdentityProviders } from "../routes/IdentityProviders";
import { GeneralSettings } from "./GeneralSettings";

export default function AddIdentityProvider() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { providerId } = useParams<IdentityProviderCreateParams>();
  const form = useForm<IdentityProviderRepresentation>({ mode: "onChange" });
  const serverInfo = useServerInfo();

  const providerInfo = useMemo(() => {
    const namespaces = [
      "org.keycloak.broker.social.SocialIdentityProvider",
      "org.keycloak.broker.provider.IdentityProvider",
    ];

    for (const namespace of namespaces) {
      const social = serverInfo.componentTypes?.[namespace]?.find(
        ({ id }) => id === providerId,
      );

      if (social) {
        return social;
      }
    }
  }, [serverInfo, providerId]);

  const {
    handleSubmit,
    formState: { isValid },
  } = form;

  const { addAlert, addError } = useAlerts();
  const navigate = useNavigate();
  const { realm } = useRealm();

  const onSubmit = async (provider: IdentityProviderRepresentation) => {
    try {
      await adminClient.identityProviders.create({
        ...provider,
        providerId,
        alias: provider.alias!,
      });
      addAlert(t("createIdentityProviderSuccess"), AlertVariant.success);
      navigate(
        toIdentityProvider({
          realm,
          providerId,
          alias: provider.alias!,
          tab: "settings",
        }),
      );
    } catch (error) {
      addError("createError", error);
    }
  };

  const alias = form.getValues("alias");

  if (!alias) {
    form.setValue("alias", providerId);
  }

  return (
    <>
      <ViewHeader
        titleKey={t("addIdentityProvider", {
          provider: toUpperCase(providerId),
        })}
      />
      <PageSection variant="light">
        <FormAccess
          role="manage-identity-providers"
          isHorizontal
          onSubmit={handleSubmit(onSubmit)}
        >
          <FormProvider {...form}>
            <GeneralSettings id={providerId} />
            {providerInfo && (
              <DynamicComponents
                stringify
                properties={providerInfo.properties}
              />
            )}
          </FormProvider>
          <ActionGroup>
            <Button
              isDisabled={!isValid}
              variant="primary"
              type="submit"
              data-testid="createProvider"
            >
              {t("add")}
            </Button>
            <Button
              variant="link"
              data-testid="cancel"
              component={(props) => (
                <Link {...props} to={toIdentityProviders({ realm })} />
              )}
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </>
  );
}
