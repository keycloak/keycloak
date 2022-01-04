import {
  Alert,
  AlertVariant,
  ButtonVariant,
  Divider,
  DropdownItem,
  Label,
  PageSection,
  Tab,
  Tabs,
  TabTitleText,
  Tooltip,
} from "@patternfly/react-core";
import { InfoCircleIcon } from "@patternfly/react-icons";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import _, { cloneDeep } from "lodash";
import React, { useMemo, useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useHistory, useParams } from "react-router-dom";
import { useAlerts } from "../components/alert/Alerts";
import {
  ConfirmDialogModal,
  useConfirmDialog,
} from "../components/confirm-dialog/ConfirmDialog";
import { DownloadDialog } from "../components/download-dialog/DownloadDialog";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import type { MultiLine } from "../components/multi-line-input/multi-line-convert";
import {
  ViewHeader,
  ViewHeaderBadge,
} from "../components/view-header/ViewHeader";
import { KeycloakSpinner } from "../components/keycloak-spinner/KeycloakSpinner";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { RolesList } from "../realm-roles/RolesList";
import {
  convertFormValuesToObject,
  convertToFormValues,
  exportClient,
} from "../util";
import useToggle from "../utils/useToggle";
import { AdvancedTab } from "./AdvancedTab";
import { ClientSettings } from "./ClientSettings";
import { Credentials } from "./credentials/Credentials";
import { Keys } from "./keys/Keys";
import type { ClientParams } from "./routes/Client";
import { toClients } from "./routes/Clients";
import { ClientScopes } from "./scopes/ClientScopes";
import { EvaluateScopes } from "./scopes/EvaluateScopes";
import { ServiceAccount } from "./service-account/ServiceAccount";
import { isRealmClient, getProtocolName } from "./utils";
import { SamlKeys } from "./keys/SamlKeys";
import { MapperList } from "../client-scopes/details/MapperList";
import type { ProtocolMapperTypeRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepesentation";
import type ProtocolMapperRepresentation from "@keycloak/keycloak-admin-client/lib/defs/protocolMapperRepresentation";
import { toMapper } from "./routes/Mapper";
import { AuthorizationSettings } from "./authorization/Settings";
import { AuthorizationResources } from "./authorization/Resources";
import { AuthorizationScopes } from "./authorization/Scopes";

type ClientDetailHeaderProps = {
  onChange: (value: boolean) => void;
  value: boolean;
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

  const dropdownItems = [
    <DropdownItem key="download" onClick={toggleDownloadDialog}>
      {t("downloadAdapterConfig")}
    </DropdownItem>,
    <DropdownItem key="export" onClick={() => exportClient(client)}>
      {t("common:export")}
    </DropdownItem>,
    ...(!isRealmClient(client)
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
        titleKey={client ? client.clientId! : ""}
        subKey="clients:clientsExplain"
        badges={badges}
        divider={false}
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

export type ClientForm = Omit<
  ClientRepresentation,
  "redirectUris" | "webOrigins"
> & {
  redirectUris: MultiLine[];
  webOrigins: MultiLine[];
};

export type SaveOptions = {
  confirmed?: boolean;
  messageKey?: string;
};

export default function ClientDetails() {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const history = useHistory();

  const [downloadDialogOpen, toggleDownloadDialogOpen] = useToggle();
  const [changeAuthenticatorOpen, toggleChangeAuthenticatorOpen] = useToggle();
  const [clientScopeSubTab, setClientScopeSubTab] = useState(30);
  const [authorizationSubTab, setAuthorizationSubTab] = useState(40);

  const form = useForm<ClientForm>({ shouldUnregister: false });
  const { clientId } = useParams<ClientParams>();

  const clientAuthenticatorType = useWatch({
    control: form.control,
    name: "clientAuthenticatorType",
    defaultValue: "client-secret",
  });

  const [client, setClient] = useState<ClientRepresentation>();

  const loader = async () => {
    const roles = await adminClient.clients.listRoles({ id: clientId });
    return _.sortBy(roles, (role) => role.name?.toUpperCase());
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
        history.push(toClients({ realm }));
      } catch (error) {
        addError("clients:clientDeleteError", error);
      }
    },
  });

  const setupForm = (client: ClientRepresentation) => {
    convertToFormValues(client, form.setValue, ["redirectUris", "webOrigins"]);
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
    [clientId]
  );

  const save = async (
    { confirmed = false, messageKey = "clientSaveSuccess" }: SaveOptions = {
      confirmed: false,
      messageKey: "clientSaveSuccess",
    }
  ) => {
    if (await form.trigger()) {
      if (
        !client?.publicClient &&
        client?.clientAuthenticatorType !== clientAuthenticatorType &&
        !confirmed
      ) {
        toggleChangeAuthenticatorOpen();
        return;
      }
      const submittedClient = convertFormValuesToObject(form.getValues(), [
        "redirectUris",
        "webOrigins",
      ]);

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
    }
  };

  const addMappers = async (
    mappers: ProtocolMapperTypeRepresentation | ProtocolMapperRepresentation[]
  ): Promise<void> => {
    if (!Array.isArray(mappers)) {
      const mapper = mappers as ProtocolMapperTypeRepresentation;
      history.push(
        toMapper({
          realm,
          id: client!.id!,
          mapperId: mapper.id!,
        })
      );
    } else {
      try {
        await adminClient.clients.addMultipleProtocolMappers(
          { id: client!.id! },
          mappers as ProtocolMapperRepresentation[]
        );
        setClient(await adminClient.clients.findOne({ id: client!.id! }));
        addAlert(t("common:mappingCreatedSuccess"), AlertVariant.success);
      } catch (error) {
        addError("common:mappingCreatedError", error);
      }
    }
  };

  const onDeleteMapper = async (mapper: ProtocolMapperRepresentation) => {
    try {
      await adminClient.clients.delProtocolMapper({
        id: client!.id!,
        mapperId: mapper.id!,
      });
      setClient({
        ...client,
        protocolMappers: client?.protocolMappers?.filter(
          (m) => m.id !== mapper.id
        ),
      });
      addAlert(t("common:mappingDeletedSuccess"), AlertVariant.success);
    } catch (error) {
      addError("common:mappingDeletedError", error);
    }
    return true;
  };

  if (!client) {
    return <KeycloakSpinner />;
  }

  return (
    <>
      <ConfirmDialogModal
        continueButtonLabel="common:yes"
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
          {clientAuthenticatorType === "client-jwt" && (
            <Alert variant="info" isInline title={t("signedJWTConfirm")} />
          )}
        </>
      </ConfirmDialogModal>
      <DeleteConfirm />
      <DownloadDialog
        id={client.id!}
        protocol={client.protocol}
        open={downloadDialogOpen}
        toggleDialog={toggleDownloadDialogOpen}
      />
      <Controller
        name="enabled"
        control={form.control}
        defaultValue={true}
        render={({ onChange, value }) => (
          <ClientDetailHeader
            value={value}
            onChange={onChange}
            client={client}
            save={save}
            toggleDeleteDialog={toggleDeleteDialog}
            toggleDownloadDialog={toggleDownloadDialogOpen}
          />
        )}
      />
      <PageSection variant="light" className="pf-u-p-0">
        <FormProvider {...form}>
          <KeycloakTabs data-testid="client-tabs" isBox mountOnEnter>
            <Tab
              id="settings"
              eventKey="settings"
              title={<TabTitleText>{t("common:settings")}</TabTitleText>}
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
                eventKey="keys"
                title={<TabTitleText>{t("keys")}</TabTitleText>}
              >
                {client.protocol === "openid-connect" && (
                  <Keys clientId={clientId} save={save} />
                )}
                {client.protocol === "saml" && (
                  <SamlKeys clientId={clientId} save={save} />
                )}
              </Tab>
            )}
            {!client.publicClient && !isRealmClient(client) && (
              <Tab
                id="credentials"
                eventKey="credentials"
                title={<TabTitleText>{t("credentials")}</TabTitleText>}
              >
                <Credentials clientId={clientId} save={() => save()} />
              </Tab>
            )}
            {!isRealmClient(client) && (
              <Tab
                id="mappers"
                eventKey="mappers"
                title={<TabTitleText>{t("mappers")}</TabTitleText>}
              >
                <MapperList
                  model={client}
                  onAdd={addMappers}
                  onDelete={onDeleteMapper}
                  detailLink={(mapperId) =>
                    toMapper({ realm, id: client.id!, mapperId })
                  }
                />
              </Tab>
            )}
            <Tab
              id="roles"
              eventKey="roles"
              title={<TabTitleText>{t("roles")}</TabTitleText>}
            >
              <RolesList
                loader={loader}
                paginated={false}
                messageBundle="clients"
              />
            </Tab>
            {!isRealmClient(client) && (
              <Tab
                id="clientScopes"
                eventKey="clientScopes"
                title={<TabTitleText>{t("clientScopes")}</TabTitleText>}
              >
                <Tabs
                  activeKey={clientScopeSubTab}
                  onSelect={(_, key) => setClientScopeSubTab(key as number)}
                >
                  <Tab
                    id="setup"
                    eventKey={30}
                    title={<TabTitleText>{t("setup")}</TabTitleText>}
                  >
                    <ClientScopes
                      clientName={client.clientId!}
                      clientId={clientId}
                      protocol={client!.protocol!}
                    />
                  </Tab>
                  <Tab
                    id="evaluate"
                    eventKey={31}
                    title={<TabTitleText>{t("evaluate")}</TabTitleText>}
                  >
                    <EvaluateScopes
                      clientId={clientId}
                      protocol={client!.protocol!}
                    />
                  </Tab>
                </Tabs>
              </Tab>
            )}
            {client!.serviceAccountsEnabled && (
              <Tab
                id="authorization"
                eventKey="authorization"
                title={<TabTitleText>{t("authorization")}</TabTitleText>}
              >
                <Tabs
                  activeKey={authorizationSubTab}
                  onSelect={(_, key) => setAuthorizationSubTab(key as number)}
                  mountOnEnter
                  unmountOnExit
                >
                  <Tab
                    id="settings"
                    eventKey={40}
                    title={<TabTitleText>{t("settings")}</TabTitleText>}
                  >
                    <AuthorizationSettings clientId={clientId} />
                  </Tab>
                  <Tab
                    id="resources"
                    eventKey={41}
                    title={<TabTitleText>{t("resources")}</TabTitleText>}
                  >
                    <AuthorizationResources clientId={clientId} />
                  </Tab>
                  <Tab
                    id="scopes"
                    eventKey={42}
                    title={<TabTitleText>{t("scopes")}</TabTitleText>}
                  >
                    <AuthorizationScopes clientId={clientId} />
                  </Tab>
                </Tabs>
              </Tab>
            )}
            {client!.serviceAccountsEnabled && (
              <Tab
                id="serviceAccount"
                eventKey="serviceAccount"
                title={<TabTitleText>{t("serviceAccount")}</TabTitleText>}
              >
                <ServiceAccount client={client} />
              </Tab>
            )}
            <Tab
              id="advanced"
              eventKey="advanced"
              title={<TabTitleText>{t("advanced")}</TabTitleText>}
            >
              <AdvancedTab save={save} client={client} />
            </Tab>
          </KeycloakTabs>
        </FormProvider>
      </PageSection>
    </>
  );
}
