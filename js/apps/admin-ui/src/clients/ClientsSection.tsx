import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ClientQuery } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import { useAlerts, useEnvironment } from "@keycloak/keycloak-ui-shared";
import {
  AlertVariant,
  Badge,
  Button,
  ButtonVariant,
  PageSection,
  Tab,
  TabTitleText,
  ToolbarItem,
  Tooltip,
} from "@patternfly/react-core";
import { WarningTriangleIcon } from "@patternfly/react-icons";
import { IRowData, TableText, cellWidth } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { FormattedLink } from "../components/external-link/FormattedLink";
import {
  RoutableTabs,
  useRoutableTab,
} from "../components/routable-tabs/RoutableTabs";
import { Action, KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { Environment } from "../environment";
import helpUrls from "../help-urls";
import { emptyFormatter, exportClient } from "../util";
import { convertClientToUrl } from "../utils/client-url";
import { translationFormatter } from "../utils/translationFormatter";
import { InitialAccessTokenList } from "./initial-access/InitialAccessTokenList";
import { ClientRegistration } from "./registration/ClientRegistration";
import { toAddClient } from "./routes/AddClient";
import { toClient } from "./routes/Client";
import { ClientsTab, toClients } from "./routes/Clients";
import { toImportClient } from "./routes/ImportClient";
import { getProtocolName, isRealmClient } from "./utils";

const ClientDetailLink = (client: ClientRepresentation) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  return (
    <TableText wrapModifier="truncate">
      <Link
        key={client.id}
        to={toClient({ realm, clientId: client.id!, tab: "settings" })}
      >
        {client.clientId}
        {!client.enabled && (
          <Badge key={`${client.id}-disabled`} isRead className="pf-v5-u-ml-sm">
            {t("disabled")}
          </Badge>
        )}
      </Link>
      {client.attributes?.["is_temporary_admin"] === "true" && (
        <Tooltip content={t("temporaryService")}>
          <WarningTriangleIcon
            className="pf-v5-u-ml-sm"
            id="temporary-admin-label"
          />
        </Tooltip>
      )}
    </TableText>
  );
};

const ClientName = (client: ClientRepresentation) => {
  const { t } = useTranslation();
  return (
    <TableText wrapModifier="truncate">
      {translationFormatter(t)(client.name) as string}
    </TableText>
  );
};

const ClientDescription = (client: ClientRepresentation) => (
  <TableText wrapModifier="truncate">
    {emptyFormatter()(client.description) as string}
  </TableText>
);

const ClientHomeLink = (client: ClientRepresentation) => {
  const { environment } = useEnvironment<Environment>();
  const href = convertClientToUrl(client, environment);

  if (!href) {
    return "—";
  }

  return (
    <FormattedLink
      href={href}
      data-testid={`client-home-url-${client.clientId}`}
    />
  );
};

const ToolbarItems = () => {
  const { t } = useTranslation();
  const { realm } = useRealm();

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients");

  if (!isManager) return <span />;

  return (
    <>
      <ToolbarItem>
        <Button
          data-testid="createClient"
          component={(props) => <Link {...props} to={toAddClient({ realm })} />}
        >
          {t("createClient")}
        </Button>
      </ToolbarItem>
      <ToolbarItem>
        <Button
          component={(props) => (
            <Link {...props} to={toImportClient({ realm })} />
          )}
          variant="link"
          data-testid="importClient"
        >
          {t("importClient")}
        </Button>
      </ToolbarItem>
    </>
  );
};

export default function ClientsSection() {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm } = useRealm();

  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const [selectedClient, setSelectedClient] = useState<ClientRepresentation>();

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients");

  const loader = async (first?: number, max?: number, search?: string) => {
    const params: ClientQuery = {
      first: first!,
      max: max!,
    };
    if (search) {
      params.clientId = search;
      params.search = true;
    }
    return adminClient.clients.find({ ...params });
  };

  const useTab = (tab: ClientsTab) => useRoutableTab(toClients({ realm, tab }));

  const listTab = useTab("list");
  const initialAccessTokenTab = useTab("initial-access-token");
  const clientRegistrationTab = useTab("client-registration");

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("clientDelete", { clientId: selectedClient?.clientId }),
    messageKey: "clientDeleteConfirm",
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.clients.del({
          id: selectedClient!.id!,
        });
        addAlert(t("clientDeletedSuccess"), AlertVariant.success);
        refresh();
      } catch (error) {
        addError("clientDeleteError", error);
      }
    },
  });

  return (
    <>
      <ViewHeader
        titleKey="clientList"
        subKey="clientsExplain"
        helpUrl={helpUrls.clientsUrl}
        divider={false}
      />
      <PageSection variant="light" className="pf-v5-u-p-0">
        <RoutableTabs
          mountOnEnter
          unmountOnExit
          isBox
          defaultLocation={toClients({
            realm,
            tab: "list",
          })}
        >
          <Tab
            data-testid="list"
            title={<TabTitleText>{t("clientsList")}</TabTitleText>}
            {...listTab}
          >
            <DeleteConfirm />
            <KeycloakDataTable
              key={key}
              loader={loader}
              isPaginated
              ariaLabelKey="clientList"
              searchPlaceholderKey="searchForClient"
              toolbarItem={<ToolbarItems />}
              actionResolver={(rowData: IRowData) => {
                const client: ClientRepresentation = rowData.data;
                const actions: Action<ClientRepresentation>[] = [
                  {
                    title: t("export"),
                    onClick() {
                      exportClient(client);
                    },
                  },
                ];

                if (
                  !isRealmClient(client) &&
                  (isManager || client.access?.configure)
                ) {
                  actions.push({
                    title: t("delete"),
                    onClick() {
                      setSelectedClient(client);
                      toggleDeleteDialog();
                    },
                  });
                }

                return actions;
              }}
              columns={[
                {
                  name: "clientId",
                  displayKey: "clientId",
                  transforms: [cellWidth(20)],
                  cellRenderer: ClientDetailLink,
                },
                {
                  name: "clientName",
                  displayKey: "clientName",
                  transforms: [cellWidth(20)],
                  cellRenderer: ClientName,
                },
                {
                  name: "protocol",
                  displayKey: "type",
                  transforms: [cellWidth(10)],
                  cellRenderer: (client) =>
                    getProtocolName(t, client.protocol ?? "openid-connect"),
                },
                {
                  name: "description",
                  displayKey: "description",
                  transforms: [cellWidth(30)],
                  cellRenderer: ClientDescription,
                },
                {
                  name: "baseUrl",
                  displayKey: "homeURL",
                  transforms: [cellWidth(20)],
                  cellRenderer: ClientHomeLink,
                },
              ]}
            />
          </Tab>
          <Tab
            data-testid="initialAccessToken"
            title={<TabTitleText>{t("initialAccessToken")}</TabTitleText>}
            {...initialAccessTokenTab}
          >
            <InitialAccessTokenList />
          </Tab>
          <Tab
            data-testid="registration"
            title={<TabTitleText>{t("clientRegistration")}</TabTitleText>}
            {...clientRegistrationTab}
          >
            <ClientRegistration />
          </Tab>
        </RoutableTabs>
      </PageSection>
    </>
  );
}
