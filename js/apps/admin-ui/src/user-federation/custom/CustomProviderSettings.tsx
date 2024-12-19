import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  KeycloakSpinner,
  TextControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  PageSection,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { FormAccess } from "../../components/form/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { useParams } from "../../utils/useParams";
import type { CustomUserFederationRouteParams } from "../routes/CustomUserFederation";
import { toUserFederation } from "../routes/UserFederation";
import { ExtendedHeader } from "../shared/ExtendedHeader";
import { SettingsCache } from "../shared/SettingsCache";
import { SyncSettings } from "./SyncSettings";

import "./custom-provider-settings.css";
import { useState } from "react";

export default function CustomProviderSettings() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { id, providerId } = useParams<CustomUserFederationRouteParams>();
  const navigate = useNavigate();
  const form = useForm<ComponentRepresentation>({
    mode: "onChange",
  });
  const {
    reset,
    setValue,
    handleSubmit,
    formState: { isDirty },
  } = form;

  const { addAlert, addError } = useAlerts();
  const { realm: realmName, realmRepresentation: realm } = useRealm();
  const [loading, setLoading] = useState(true);

  const provider = (
    useServerInfo().componentTypes?.[
      "org.keycloak.storage.UserStorageProvider"
    ] || []
  ).find((p) => p.id === providerId);

  useFetch(
    async () => {
      if (id) {
        return await adminClient.components.findOne({ id });
      }
      return undefined;
    },
    (fetchedComponent) => {
      if (fetchedComponent) {
        convertToFormValues(fetchedComponent, setValue);
      } else if (id) {
        throw new Error(t("notFound"));
      }
      setLoading(false);
    },
    [],
  );

  const save = async (component: ComponentRepresentation) => {
    const saveComponent = convertFormValuesToObject({
      ...component,
      config: Object.fromEntries(
        Object.entries(component.config || {}).map(([key, value]) => [
          key,
          Array.isArray(value) ? value : [value],
        ]),
      ),
      providerId,
      providerType: "org.keycloak.storage.UserStorageProvider",
      parentId: realm?.id,
    });

    try {
      if (!id) {
        await adminClient.components.create(saveComponent);
        navigate(toUserFederation({ realm: realmName }));
      } else {
        await adminClient.components.update({ id }, saveComponent);
      }
      reset({ ...component });
      addAlert(
        t(!id ? "createUserProviderSuccess" : "userProviderSaveSuccess"),
        AlertVariant.success,
      );
    } catch (error) {
      addError(
        !id ? "createUserProviderError" : "userProviderSaveError",
        error,
      );
    }
  };

  if (loading) return <KeycloakSpinner />;

  return (
    <FormProvider {...form}>
      <ExtendedHeader provider={providerId} save={() => handleSubmit(save)()} />
      <PageSection variant="light">
        <FormAccess
          role="manage-realm"
          isHorizontal
          className="keycloak__user-federation__custom-form"
          onSubmit={handleSubmit(save)}
        >
          <TextControl
            name="name"
            label={t("uiDisplayName")}
            labelIcon={t("uiDisplayNameHelp")}
            rules={{
              required: t("validateName"),
            }}
          />
          <DynamicComponents properties={provider?.properties || []} />
          {provider?.metadata.synchronizable && <SyncSettings />}
          <SettingsCache form={form} unWrap />
          <ActionGroup>
            <Button
              isDisabled={!isDirty}
              variant="primary"
              type="submit"
              data-testid="custom-save"
            >
              {t("save")}
            </Button>
            <Button
              variant="link"
              component={(props) => (
                <Link {...props} to={toUserFederation({ realm: realmName })} />
              )}
              data-testid="custom-cancel"
            >
              {t("cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </FormProvider>
  );
}
