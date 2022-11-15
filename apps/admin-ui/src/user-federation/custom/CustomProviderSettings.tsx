import { useState } from "react";
import { useParams } from "react-router-dom";
import { Link, useNavigate } from "react-router-dom-v5-compat";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";
import {
  ActionGroup,
  AlertVariant,
  Button,
  FormGroup,
  PageSection,
} from "@patternfly/react-core";

import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type { ProviderRouteParams } from "../routes/NewProvider";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { FormAccess } from "../../components/form-access/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { toUserFederation } from "../routes/UserFederation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";
import { SettingsCache } from "../shared/SettingsCache";
import { ExtendedHeader } from "../shared/ExtendedHeader";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { DynamicComponents } from "../../components/dynamic/DynamicComponents";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { SyncSettings } from "./SyncSettings";

import "./custom-provider-settings.css";

export default function CustomProviderSettings() {
  const { t } = useTranslation("user-federation");
  const { id, providerId } = useParams<ProviderRouteParams>();
  const navigate = useNavigate();
  const form = useForm<ComponentRepresentation>({
    shouldUnregister: false,
    mode: "onChange",
  });
  const {
    register,
    errors,
    reset,
    setValue,
    handleSubmit,
    formState: { isDirty },
  } = form;

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();
  const [parentId, setParentId] = useState("");

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
        throw new Error(t("common:notFound"));
      }
    },
    []
  );

  useFetch(
    () =>
      adminClient.realms.findOne({
        realm: realmName,
      }),
    (realm) => setParentId(realm?.id!),
    []
  );

  const save = async (component: ComponentRepresentation) => {
    const saveComponent = convertFormValuesToObject({
      ...component,
      config: Object.fromEntries(
        Object.entries(component.config || {}).map(([key, value]) => [
          key,
          Array.isArray(value) ? value : [value],
        ])
      ),
      providerId,
      providerType: "org.keycloak.storage.UserStorageProvider",
      parentId,
    });

    try {
      if (!id) {
        await adminClient.components.create(saveComponent);
        navigate(toUserFederation({ realm: realmName }));
      } else {
        await adminClient.components.update({ id }, saveComponent);
      }
      reset({ ...component });
      addAlert(t(!id ? "createSuccess" : "saveSuccess"), AlertVariant.success);
    } catch (error) {
      addError(`user-federation:${!id ? "createError" : "saveError"}`, error);
    }
  };

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
          <FormGroup
            label={t("consoleDisplayName")}
            labelIcon={
              <HelpItem
                helpText="user-federation-help:consoleDisplayNameHelp"
                fieldLabelId="user-federation:consoleDisplayName"
              />
            }
            helperTextInvalid={t("validateName")}
            validated={errors.name ? "error" : "default"}
            fieldId="kc-console-display-name"
            isRequired
          >
            <KeycloakTextInput
              isRequired
              type="text"
              id="kc-console-display-name"
              name="name"
              ref={register({
                required: true,
              })}
              data-testid="console-name"
              validated={errors.name ? "error" : "default"}
            />
          </FormGroup>
          <FormProvider {...form}>
            <DynamicComponents properties={provider?.properties || []} />
            {provider?.metadata.synchronizable && <SyncSettings />}
          </FormProvider>
          <SettingsCache form={form} unWrap />
          <ActionGroup>
            <Button
              isDisabled={!isDirty}
              variant="primary"
              type="submit"
              data-testid="custom-save"
            >
              {t("common:save")}
            </Button>
            <Button
              variant="link"
              component={(props) => (
                <Link {...props} to={toUserFederation({ realm: realmName })} />
              )}
              data-testid="custom-cancel"
            >
              {t("common:cancel")}
            </Button>
          </ActionGroup>
        </FormAccess>
      </PageSection>
    </FormProvider>
  );
}
