import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useAdminClient } from "../admin-client";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { PermissionsResources } from "./PermissionsResources";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import {
  PermissionsTabs,
  toPermissionsTabs,
} from "../permissions/routes/PermissionsTabs";
import {
  AlertVariant,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { PermissionsExport } from "../permissions/PermissionsExport";
import { PermissionsEvaluate } from "../permissions/PermissionsEvaluate";
import { PermissionsList } from "../permissions/PermissionsList";
import { PermissionsPolicies } from "../permissions/PermissionsPolicies";
import { PermissionsScopes } from "../permissions/PermissionsScopes";
import { PermissionsSettings } from "../permissions/PermissionsSettings";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAccess } from "../context/access/Access";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { FormFields, SaveOptions } from "../clients/ClientDetails";
import {
  convertAttributeNameToForm,
  convertFormValuesToObject,
  convertToFormValues,
} from "../util";
import { ConfirmDialogModal } from "../components/confirm-dialog/ConfirmDialog";
import { KeyValueType } from "../components/key-value-form/key-value-convert";
import useToggle from "../utils/useToggle";

export default function PermissionsSection() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { hasAccess } = useAccess();
  const { addAlert, addError } = useAlerts();
  const [realmManagementClient, setRealmManagementClient] = useState<
    ClientRepresentation | undefined
  >();
  const [changeAuthenticatorOpen, toggleChangeAuthenticatorOpen] = useToggle();
  const form = useForm<FormFields>();

  const usePermissionsTabs = (tab: PermissionsTabs) =>
    useRoutableTab(
      toPermissionsTabs({
        realm,
        tab,
      }),
    );

  const clientAuthenticatorType = useWatch({
    control: form.control,
    name: "clientAuthenticatorType",
    defaultValue: "client-secret",
  });

  const hasManageAuthorization = hasAccess("manage-authorization");
  const hasViewUsers = hasAccess("view-users");
  const permissionsSettingsTab = usePermissionsTabs("settings");
  const permissionsResourcesTab = usePermissionsTabs("resources");
  const permissionsScopesTab = usePermissionsTabs("scopes");
  const permissionsPoliciesTab = usePermissionsTabs("policies");
  const permissionsPermissionsTab = usePermissionsTabs("permissions");
  const permissionsEvaluateTab = usePermissionsTabs("evaluate");
  const permissionsExportTab = usePermissionsTabs("export");

  useFetch(
    async () => {
      const clients = await adminClient.clients.find();
      return clients;
    },
    (clients) => {
      const realmManagementClient = clients.find(
        (client) => client.clientId === "realm-management",
      );
      setRealmManagementClient(realmManagementClient!);
    },
    [],
  );

  const setupForm = (client: ClientRepresentation) => {
    form.reset({ ...client });
    convertToFormValues(client, form.setValue);
    if (client.attributes?.["acr.loa.map"]) {
      form.setValue(
        convertAttributeNameToForm("attributes.acr.loa.map"),
        // @ts-ignore
        Object.entries(JSON.parse(client.attributes["acr.loa.map"])).flatMap(
          ([key, value]) => ({ key, value }),
        ),
      );
    }
  };

  const save = async (
    { confirmed = false, messageKey = "clientSaveSuccess" }: SaveOptions = {
      confirmed: false,
      messageKey: "clientSaveSuccess",
    },
  ) => {
    if (!(await form.trigger())) {
      return;
    }

    if (
      !realmManagementClient?.publicClient &&
      realmManagementClient?.clientAuthenticatorType !==
        clientAuthenticatorType &&
      !confirmed
    ) {
      toggleChangeAuthenticatorOpen();
      return;
    }

    const values = convertFormValuesToObject(form.getValues());

    const submittedClient =
      convertFormValuesToObject<ClientRepresentation>(values);

    if (submittedClient.attributes?.["acr.loa.map"]) {
      submittedClient.attributes["acr.loa.map"] = JSON.stringify(
        Object.fromEntries(
          (submittedClient.attributes["acr.loa.map"] as KeyValueType[])
            .filter(({ key }) => key !== "")
            .map(({ key, value }) => [key, value]),
        ),
      );
    }

    try {
      const newClient: ClientRepresentation = {
        ...realmManagementClient,
        ...submittedClient,
      };

      newClient.clientId = newClient.clientId?.trim();

      await adminClient.clients.update(
        { id: realmManagementClient!.clientId! },
        newClient,
      );
      setupForm(newClient);
      setRealmManagementClient(newClient);
      addAlert(t(messageKey), AlertVariant.success);
    } catch (error) {
      addError("clientSaveError", error);
    }
  };

  return (
    realmManagementClient && (
      <>
        <ConfirmDialogModal
          continueButtonLabel="yes"
          cancelButtonLabel="no"
          titleKey={t("changeAuthenticatorConfirmTitle", {
            clientAuthenticatorType: clientAuthenticatorType,
          })}
          open={changeAuthenticatorOpen}
          toggleDialog={toggleChangeAuthenticatorOpen}
          onConfirm={() => save({ confirmed: true })}
        >
          <>
            {t("changeAuthenticatorConfirm", {
              clientAuthenticatorType: clientAuthenticatorType,
            })}
          </>
        </ConfirmDialogModal>
        <PageSection variant="light" className="pf-v5-u-p-0">
          <FormProvider {...form}>
            <ViewHeader titleKey={realmManagementClient.clientId!} />
            <RoutableTabs
              mountOnEnter
              unmountOnExit
              defaultLocation={toPermissionsTabs({
                realm,
                tab: "settings",
              })}
            >
              <Tab
                id="settings"
                data-testid="permissionsSettings"
                title={<TabTitleText>{t("settings")}</TabTitleText>}
                {...permissionsSettingsTab}
              >
                <PermissionsSettings clientId={realmManagementClient.id!} />
              </Tab>
              <Tab
                id="resources"
                data-testid="permissionsResources"
                title={<TabTitleText>{t("resources")}</TabTitleText>}
                {...permissionsResourcesTab}
              >
                <PermissionsResources clientId={realmManagementClient.id!} />
              </Tab>
              <Tab
                id="scopes"
                data-testid="permissionsScopes"
                title={<TabTitleText>{t("scopes")}</TabTitleText>}
                {...permissionsScopesTab}
              >
                <PermissionsScopes
                  clientId={realmManagementClient.id!}
                  isDisabled={!hasManageAuthorization}
                />
              </Tab>
              <Tab
                id="policies"
                data-testid="permissionsPolicies"
                title={<TabTitleText>{t("policies")}</TabTitleText>}
                {...permissionsPoliciesTab}
              >
                <PermissionsPolicies
                  clientId={realmManagementClient.id!}
                  isDisabled={!hasManageAuthorization}
                />
              </Tab>
              <Tab
                id="permissions"
                data-testid="permissionsPermissions"
                title={<TabTitleText>{t("permissions")}</TabTitleText>}
                {...permissionsPermissionsTab}
              >
                <PermissionsList
                  clientId={realmManagementClient.id!}
                  isDisabled={!hasManageAuthorization}
                />
              </Tab>
              {hasViewUsers && (
                <Tab
                  id="evaluate"
                  data-testid="permissionsEvaluate"
                  title={<TabTitleText>{t("evaluate")}</TabTitleText>}
                  {...permissionsEvaluateTab}
                >
                  <PermissionsEvaluate
                    client={realmManagementClient}
                    save={save}
                  />
                </Tab>
              )}
              {hasAccess("manage-authorization") && (
                <Tab
                  id="export"
                  data-testid="permissionsExport"
                  title={<TabTitleText>{t("export")}</TabTitleText>}
                  {...permissionsExportTab}
                >
                  <PermissionsExport clientId={realmManagementClient.id!} />
                </Tab>
              )}
            </RoutableTabs>
          </FormProvider>
        </PageSection>
      </>
    )
  );
}
