import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  AlertVariant,
  ButtonVariant,
  Divider,
  DropdownItem,
  Label,
  PageSection,
  Tab,
  TabTitleText,
  Tooltip,
} from "@patternfly/react-core";
import { InfoCircleIcon } from "@patternfly/react-icons";
import { cloneDeep, sortBy } from "lodash-es";
import { useMemo, useState } from "react";
import {
  Controller,
  FormProvider,
  useForm,
  useWatch,
} from "react-hook-form-v7";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { useNavigate } from "react-router-dom-v5-compat";

import { useAlerts } from "../components/alert/Alerts";
import {
  ConfirmDialogModal,
  useConfirmDialog,
} from "../components/confirm-dialog/ConfirmDialog";
import { DownloadDialog } from "../components/download-dialog/DownloadDialog";
import type { KeyValueType } from "../components/key-value-form/key-value-convert";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { PermissionsTab } from "../components/permission-tab/PermissionTab";
import {
  routableTab,
  RoutableTabs,
} from "../components/routable-tabs/RoutableTabs";
import {
  ViewHeader,
  ViewHeaderBadge,
} from "../components/view-header/ViewHeader";
import { useAccess } from "../context/access/Access";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { RolesList } from "../realm-roles/RolesList";
import {
  convertAttributeNameToForm,
  convertFormValuesToObject,
  convertToFormValues,
  exportClient,
} from "../util";
import { useParams } from "../utils/useParams";
import useToggle from "../utils/useToggle";
import { AdvancedTab } from "./AdvancedTab";
import { AuthorizationEvaluate } from "./authorization/AuthorizationEvaluate";
import { AuthorizationExport } from "./authorization/AuthorizationExport";
import { AuthorizationPermissions } from "./authorization/Permissions";
import { AuthorizationPolicies } from "./authorization/Policies";
import { AuthorizationResources } from "./authorization/Resources";
import { AuthorizationScopes } from "./authorization/Scopes";
import { AuthorizationSettings } from "./authorization/Settings";
import { ClientSessions } from "./ClientSessions";
import { ClientSettings } from "./ClientSettings";
import { Credentials } from "./credentials/Credentials";
import { Keys } from "./keys/Keys";
import { SamlKeys } from "./keys/SamlKeys";
import {
  AuthorizationTab,
  toAuthorizationTab,
} from "./routes/AuthenticationTab";
import { ClientParams, ClientTab, toClient } from "./routes/Client";
import { toClients } from "./routes/Clients";
import { toClientScopesTab } from "./routes/ClientScopeTab";
import { ClientScopes } from "./scopes/ClientScopes";
import { EvaluateScopes } from "./scopes/EvaluateScopes";
import { ServiceAccount } from "./service-account/ServiceAccount";
import { getProtocolName, isRealmClient } from "./utils";

type ClientDetailHeaderProps = {
  onChange: (value: boolean) => void;
  value: boolean | undefined;
  save: () => void;
  client: ClientRepresentation;
  toggleDownloadDialog: () => void;
  toggleDeleteDialog: () => void;
};

const ClientDetailHeader = ({
  onChange,
  value,
  save,
  client,
  toggleDownloadDialog,
  toggleDeleteDialog,
}: ClientDetailHeaderProps) => {
  const { t } = useTranslation("clients");
  const [toggleDisableDialog, DisableConfirm] = useConfirmDialog({
    titleKey: "clients:disableConfirmTitle",
    messageKey: "clients:disableConfirm",
    continueButtonLabel: "common:disable",
    onConfirm: () => {
      onChange(!value);
      save();
    },
  });

  const badges = useMemo<ViewHeaderBadge[]>(() => {
    const protocolName = getProtocolName(
      t,
      client.protocol ?? "openid-connect"
    );

    const text = client.bearerOnly ? (
      <Tooltip
        data-testid="bearer-only-explainer-tooltip"
        content={t("explainBearerOnly")}
      >
        <Label
          data-testid="bearer-only-explainer-label"
          icon={<InfoCircleIcon />}
        >
          {protocolName}
        </Label>
      </Tooltip>
    ) : (
      <Label>{protocolName}</Label>
    );

    return [{ text }];
  }, [client, t]);

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients") || client.access?.configure;

  const dropdownItems = [
    <DropdownItem key="download" onClick={toggleDownloadDialog}>
      {t("downloadAdapterConfig")}
    </DropdownItem>,
    <DropdownItem key="export" onClick={() => exportClient(client)}>
      {t("common:export")}
    </DropdownItem>,
    ...(!isRealmClient(client) && isManager
      ? [
          <Divider key="divider" />,
          <DropdownItem
            data-testid="delete-client"
            key="delete"
            onClick={toggleDeleteDialog}
          >
            {t("common:delete")}
          </DropdownItem>,
        ]
      : []),
  ];

  return (
    <>
      <DisableConfirm />
      <ViewHeader
        titleKey={client.clientId!}
        subKey="clients:clientsExplain"
        badges={badges}
        divider={false}
        isReadOnly={!isManager}
        helpTextKey="clients-help:enableDisable"
        dropdownItems={dropdownItems}
        isEnabled={value}
        onToggle={(value) => {
          if (!value) {
            toggleDisableDialog();
          } else {
            onChange(value);
            save();
          }
        }}
      />
    </>
  );
};

export type SaveOptions = {
  confirmed?: boolean;
  messageKey?: string;
};

export type FormFields = Omit<
  ClientRepresentation,
  "authorizationSettings" | "resources"
>;

export default function ClientDetails() {
  const { t } = useTranslation("clients");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();
  const { profileInfo } = useServerInfo();

  const { hasAccess } = useAccess();
  const permissionsEnabled =
    !profileInfo?.disabledFeatures?.includes("ADMIN_FINE_GRAINED_AUTHZ") &&
    hasAccess("manage-authorization");
  const hasManageClients = hasAccess("manage-clients");
  const hasViewUsers = hasAccess("view-users");

  const history = useHistory();
  const navigate = useNavigate();

  const [downloadDialogOpen, toggleDownloadDialogOpen] = useToggle();
  const [changeAuthenticatorOpen, toggleChangeAuthenticatorOpen] = useToggle();

  const form = useForm<FormFields>({ shouldUnregister: false });
  const { clientId } = useParams<ClientParams>();
  const [key, setKey] = useState(0);

  const clientAuthenticatorType = useWatch({
    control: form.control,
    name: "clientAuthenticatorType",
    defaultValue: "client-secret",
  });

  const [client, setClient] = useState<ClientRepresentation>();

  const loader = async () => {
    const roles = await adminClient.clients.listRoles({ id: clientId });
    return sortBy(roles, (role) => role.name?.toUpperCase());
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: "clients:clientDeleteConfirmTitle",
    messageKey: "clients:clientDeleteConfirm",
    continueButtonLabel: "common:delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clients.del({ id: clientId });
        addAlert(t("clientDeletedSuccess"), AlertVariant.success);
        navigate(toClients({ realm }));
      } catch (error) {
        addError("clients:clientDeleteError", error);
      }
    },
  });

  const setupForm = (client: ClientRepresentation) => {
    form.reset({ ...client });
    convertToFormValues(client, form.setValue);
    if (client.attributes?.["acr.loa.map"]) {
      form.setValue(
        convertAttributeNameToForm("attributes.acr.loa.map"),
        // @ts-ignore
        Object.entries(JSON.parse(client.attributes["acr.loa.map"])).flatMap(
          ([key, value]) => ({ key, value })
        )
      );
    }
  };

  useFetch(
    () => adminClient.clients.findOne({ id: clientId }),
    (fetchedClient) => {
      if (!fetchedClient) {
        throw new Error(t("common:notFound"));
      }
      setClient(cloneDeep(fetchedClient));
      setupForm(fetchedClient);
    },
    [clientId, key]
  );

  const save = async (
    { confirmed = false, messageKey = "clientSaveSuccess" }: SaveOptions = {
      confirmed: false,
      messageKey: "clientSaveSuccess",
    }
  ) => {
    if (!(await form.trigger())) {
      return;
    }

    if (
      !client?.publicClient &&
      client?.clientAuthenticatorType !== clientAuthenticatorType &&
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
            .map(({ key, value }) => [key, value])
        )
      );
    }

    try {
      const newClient: ClientRepresentation = {
        ...client,
        ...submittedClient,
      };

      newClient.clientId = newClient.clientId?.trim();

      await adminClient.clients.update({ id: clientId }, newClient);
      setupForm(newClient);
      setClient(newClient);
      addAlert(t(messageKey), AlertVariant.success);
    } catch (error) {
      addError("clients:clientSaveError", error);
    }
  };

  if (!client) {
    return <KeycloakSpinner />;
  }

  const route = (tab: ClientTab) =>
    routableTab({
      to: toClient({
        realm,
        clientId,
        tab,
      }),
      history,
    });

  const authenticationRoute = (tab: AuthorizationTab) =>
    routableTab({
      to: toAuthorizationTab({
        realm,
        clientId,
        tab,
      }),
      history,
    });

  return (
    <>
      <ConfirmDialogModal
        continueButtonLabel="common:yes"
        cancelButtonLabel="common:no"
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
      <DeleteConfirm />
      {downloadDialogOpen && (
        <DownloadDialog
          id={client.id!}
          protocol={client.protocol}
          open
          toggleDialog={toggleDownloadDialogOpen}
        />
      )}
      <Controller
        name="enabled"
        control={form.control}
        defaultValue={true}
        render={({ field }) => (
          <ClientDetailHeader
            value={field.value}
            onChange={field.onChange}
            client={client}
            save={save}
            toggleDeleteDialog={toggleDeleteDialog}
            toggleDownloadDialog={toggleDownloadDialogOpen}
          />
        )}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <FormProvider {...form}>
          <RoutableTabs data-testid="client-tabs" isBox mountOnEnter>
            <Tab
              id="settings"
              data-testid="clientSettingsTab"
              title={<TabTitleText>{t("common:settings")}</TabTitleText>}
              {...route("settings")}
            >
              <ClientSettings
                client={client}
                save={() => save()}
                reset={() => setupForm(client)}
              />
            </Tab>
            {((!client.publicClient && !isRealmClient(client)) ||
              client.protocol === "saml") && (
              <Tab
                id="keys"
                data-testid="keysTab"
                title={<TabTitleText>{t("keys")}</TabTitleText>}
                {...route("keys")}
              >
                {client.protocol === "openid-connect" && (
                  <Keys
                    clientId={clientId}
                    save={save}
                    hasConfigureAccess={client.access?.configure}
                  />
                )}
                {client.protocol === "saml" && (
                  <SamlKeys clientId={clientId} save={save} />
                )}
              </Tab>
            )}
            {!client.publicClient &&
              !isRealmClient(client) &&
              (hasManageClients || client.access?.configure) && (
                <Tab
                  id="credentials"
                  title={<TabTitleText>{t("credentials")}</TabTitleText>}
                  {...route("credentials")}
                >
                  <Credentials
                    key={key}
                    client={client}
                    save={save}
                    refresh={() => setKey(key + 1)}
                  />
                </Tab>
              )}
            <Tab
              id="roles"
              data-testid="rolesTab"
              title={<TabTitleText>{t("roles")}</TabTitleText>}
              {...route("roles")}
            >
              <RolesList
                loader={loader}
                paginated={false}
                messageBundle="clients"
                isReadOnly={!(hasManageClients || client.access?.configure)}
              />
            </Tab>
            {!isRealmClient(client) && !client.bearerOnly && (
              <Tab
                id="clientScopes"
                data-testid="clientScopesTab"
                title={<TabTitleText>{t("clientScopes")}</TabTitleText>}
                {...route("clientScopes")}
              >
                <RoutableTabs
                  defaultLocation={toClientScopesTab({
                    realm,
                    clientId,
                    tab: "setup",
                  })}
                >
                  <Tab
                    id="setup"
                    title={<TabTitleText>{t("setup")}</TabTitleText>}
                    {...routableTab({
                      to: toClientScopesTab({
                        realm,
                        clientId,
                        tab: "setup",
                      }),
                      history,
                    })}
                  >
                    <ClientScopes
                      clientName={client.clientId!}
                      clientId={clientId}
                      protocol={client!.protocol!}
                      fineGrainedAccess={client!.access?.manage}
                    />
                  </Tab>
                  <Tab
                    id="evaluate"
                    title={<TabTitleText>{t("evaluate")}</TabTitleText>}
                    {...routableTab({
                      to: toClientScopesTab({
                        realm,
                        clientId,
                        tab: "evaluate",
                      }),
                      history,
                    })}
                  >
                    <EvaluateScopes
                      clientId={clientId}
                      protocol={client!.protocol!}
                    />
                  </Tab>
                </RoutableTabs>
              </Tab>
            )}
            {client!.authorizationServicesEnabled && (
              <Tab
                id="authorization"
                data-testid="authorizationTab"
                title={<TabTitleText>{t("authorization")}</TabTitleText>}
                {...route("authorization")}
              >
                <RoutableTabs
                  mountOnEnter
                  unmountOnExit
                  defaultLocation={toAuthorizationTab({
                    realm,
                    clientId,
                    tab: "settings",
                  })}
                >
                  <Tab
                    id="settings"
                    data-testid="authorizationSettings"
                    title={<TabTitleText>{t("settings")}</TabTitleText>}
                    {...authenticationRoute("settings")}
                  >
                    <AuthorizationSettings clientId={clientId} />
                  </Tab>
                  <Tab
                    id="resources"
                    data-testid="authorizationResources"
                    title={<TabTitleText>{t("resources")}</TabTitleText>}
                    {...authenticationRoute("resources")}
                  >
                    <AuthorizationResources clientId={clientId} />
                  </Tab>
                  <Tab
                    id="scopes"
                    data-testid="authorizationScopes"
                    title={<TabTitleText>{t("scopes")}</TabTitleText>}
                    {...authenticationRoute("scopes")}
                  >
                    <AuthorizationScopes clientId={clientId} />
                  </Tab>
                  <Tab
                    id="policies"
                    data-testid="authorizationPolicies"
                    title={<TabTitleText>{t("policies")}</TabTitleText>}
                    {...authenticationRoute("policies")}
                  >
                    <AuthorizationPolicies clientId={clientId} />
                  </Tab>
                  <Tab
                    id="permissions"
                    data-testid="authorizationPermissions"
                    title={
                      <TabTitleText>{t("common:permissions")}</TabTitleText>
                    }
                    {...authenticationRoute("permissions")}
                  >
                    <AuthorizationPermissions clientId={clientId} />
                  </Tab>
                  <Tab
                    id="evaluate"
                    data-testid="authorizationEvaluate"
                    title={<TabTitleText>{t("evaluate")}</TabTitleText>}
                    {...authenticationRoute("evaluate")}
                  >
                    <AuthorizationEvaluate client={client} save={save} />
                  </Tab>
                  <Tab
                    id="export"
                    data-testid="authorizationExport"
                    title={<TabTitleText>{t("common:export")}</TabTitleText>}
                    {...authenticationRoute("export")}
                  >
                    <AuthorizationExport />
                  </Tab>
                </RoutableTabs>
              </Tab>
            )}
            {client!.serviceAccountsEnabled && hasViewUsers && (
              <Tab
                id="serviceAccount"
                data-testid="serviceAccountTab"
                title={<TabTitleText>{t("serviceAccount")}</TabTitleText>}
                {...route("serviceAccount")}
              >
                <ServiceAccount client={client} />
              </Tab>
            )}
            <Tab
              id="sessions"
              data-testid="sessionsTab"
              title={<TabTitleText>{t("sessions")}</TabTitleText>}
              {...route("sessions")}
            >
              <ClientSessions client={client} />
            </Tab>
            {permissionsEnabled &&
              (hasManageClients || client.access?.manage) && (
                <Tab
                  id="permissions"
                  data-testid="permissionsTab"
                  title={<TabTitleText>{t("common:permissions")}</TabTitleText>}
                  {...route("permissions")}
                >
                  <PermissionsTab id={client.id!} type="clients" />
                </Tab>
              )}
            <Tab
              id="advanced"
              data-testid="advancedTab"
              title={<TabTitleText>{t("advanced")}</TabTitleText>}
              {...route("advanced")}
            >
              <AdvancedTab save={save} client={client} />
            </Tab>
          </RoutableTabs>
        </FormProvider>
      </PageSection>
    </>
  );
}
