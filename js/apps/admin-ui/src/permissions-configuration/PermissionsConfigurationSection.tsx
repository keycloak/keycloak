import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { useAlerts, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  PageSection,
  Tab,
  TabTitleText,
} from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { AuthorizationPolicies } from "../clients/authorization/Policies";
import { FormFields, SaveOptions } from "../clients/ClientDetails";
import { ConfirmDialogModal } from "../components/confirm-dialog/ConfirmDialog";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { toPermissionsConfigurationTabs } from "../permissions-configuration/routes/PermissionsConfigurationTabs";
import { convertFormValuesToObject, convertToFormValues } from "../util";
import useToggle from "../utils/useToggle";
import { PermissionsConfigurationTab } from "./permission-configuration/PermissionsConfigurationTab";
import { PermissionsEvaluationTab } from "./permission-evaluation/PermissionsEvaluationTab";

export default function PermissionsConfigurationSection() {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { hasAccess } = useAccess();
  const { addAlert, addError } = useAlerts();
  const [adminPermissionsClient, setAdminPermissionsClient] = useState<
    ClientRepresentation | undefined
  >();
  const [changeAuthenticatorOpen, toggleChangeAuthenticatorOpen] = useToggle();
  const form = useForm<FormFields>();
  const { realmRepresentation } = useRealm();

  const clientAuthenticatorType = useWatch({
    control: form.control,
    name: "clientAuthenticatorType",
    defaultValue: "client-secret",
  });

  const hasManageAuthorization = hasAccess("manage-authorization");
  const hasViewUsers = hasAccess("view-users");
  const permissionsResourcesTab = useRoutableTab(
    toPermissionsConfigurationTabs({
      realm,
      permissionClientId: realmRepresentation?.adminPermissionsClient?.id!,
      tab: "permissions",
    }),
  );
  const permissionsPoliciesTab = useRoutableTab(
    toPermissionsConfigurationTabs({
      realm,
      permissionClientId: realmRepresentation?.adminPermissionsClient?.id!,
      tab: "policies",
    }),
  );
  const permissionsEvaluateTab = useRoutableTab(
    toPermissionsConfigurationTabs({
      realm,
      permissionClientId: realmRepresentation?.adminPermissionsClient?.id!,
      tab: "evaluation",
    }),
  );

  useFetch(
    async () => {
      const clients = await adminClient.clients.find({
        clientId: "admin-permissions",
      });
      return clients[0];
    },
    (adminPermissionsClient) => {
      setAdminPermissionsClient(adminPermissionsClient!);
    },
    [],
  );

  const setupForm = (client: ClientRepresentation) => {
    form.reset({ ...client });
    convertToFormValues(client, form.setValue);
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
      !adminPermissionsClient?.publicClient &&
      adminPermissionsClient?.clientAuthenticatorType !==
        clientAuthenticatorType &&
      !confirmed
    ) {
      toggleChangeAuthenticatorOpen();
      return;
    }

    const values = convertFormValuesToObject(form.getValues());

    const submittedClient =
      convertFormValuesToObject<ClientRepresentation>(values);

    try {
      const newClient: ClientRepresentation = {
        ...adminPermissionsClient,
        ...submittedClient,
      };

      newClient.clientId = newClient.clientId?.trim();

      await adminClient.clients.update(
        { id: adminPermissionsClient!.clientId! },
        newClient,
      );
      setupForm(newClient);
      setAdminPermissionsClient(newClient);
      addAlert(t(messageKey), AlertVariant.success);
    } catch (error) {
      addError("clientSaveError", error);
    }
  };

  return (
    adminPermissionsClient && (
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
            <ViewHeader
              titleKey={t("permissions")}
              subKey={t("permissionsSubTitle")}
            />
            <RoutableTabs
              mountOnEnter
              unmountOnExit
              defaultLocation={toPermissionsConfigurationTabs({
                realm,
                permissionClientId: adminPermissionsClient.id!,
                tab: "permissions",
              })}
            >
              <Tab
                id="resources"
                data-testid="permissionsResources"
                title={<TabTitleText>{t("permissions")}</TabTitleText>}
                {...permissionsResourcesTab}
              >
                <PermissionsConfigurationTab
                  clientId={adminPermissionsClient.id!}
                />
              </Tab>
              <Tab
                id="policies"
                data-testid="permissionsPolicies"
                title={<TabTitleText>{t("policies")}</TabTitleText>}
                {...permissionsPoliciesTab}
              >
                <AuthorizationPolicies
                  clientId={adminPermissionsClient.id!}
                  isDisabled={!hasManageAuthorization}
                />
              </Tab>
              {hasViewUsers && (
                <Tab
                  id="evaluation"
                  data-testid="permissionsEvaluation"
                  title={<TabTitleText>{t("evaluation")}</TabTitleText>}
                  {...permissionsEvaluateTab}
                >
                  <PermissionsEvaluationTab
                    client={adminPermissionsClient}
                    save={save}
                  />
                </Tab>
              )}
            </RoutableTabs>
          </FormProvider>
        </PageSection>
      </>
    )
  );
}
