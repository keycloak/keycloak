import type UserSessionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userSessionRepresentation";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Label,
  List,
  ListItem,
  ListVariant,
  ToolbarItem,
  Tooltip,
} from "@patternfly/react-core";
import { CubesIcon, InfoCircleIcon } from "@patternfly/react-icons";
import { IRowData } from "@patternfly/react-table";
import { ReactNode, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useMatch, useNavigate } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { toClient } from "../clients/routes/Client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import { ListEmptyState } from "@keycloak/keycloak-ui-shared";
import {
  Action,
  Field,
  KeycloakDataTable,
  LoaderFunction,
} from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { UserRoute, toUser } from "../user/routes/User";
import { toUsers } from "../user/routes/Users";
import { isLightweightUser } from "../user/utils";
import useFormatDate from "../utils/useFormatDate";

export type ColumnName =
  | "username"
  | "start"
  | "lastAccess"
  | "clients"
  | "type";

export type SessionsTableProps = {
  loader: LoaderFunction<UserSessionRepresentation>;
  hiddenColumns?: ColumnName[];
  emptyInstructions?: string;
  logoutUser?: string;
  filter?: ReactNode;
  isSearching?: boolean;
  isPaginated?: boolean;
};

const UsernameCell = (row: UserSessionRepresentation) => {
  const { realm } = useRealm();
  const { t } = useTranslation();
  return (
    <Link to={toUser({ realm, id: row.userId!, tab: "sessions" })}>
      {row.username}
      {row.transientUser && (
        <>
          {" "}
          <Tooltip content={t("transientUserTooltip")}>
            <Label
              data-testid="user-details-label-transient-user"
              icon={<InfoCircleIcon />}
              isCompact
            >
              {t("transientUser")}
            </Label>
          </Tooltip>
        </>
      )}
    </Link>
  );
};

const ClientsCell = (row: UserSessionRepresentation) => {
  const { realm } = useRealm();
  return (
    <List variant={ListVariant.inline}>
      {Object.entries(row.clients!).map(([clientId, client]) => (
        <ListItem key={clientId}>
          <Link to={toClient({ realm, clientId, tab: "sessions" })}>
            {client}
          </Link>
        </ListItem>
      ))}
    </List>
  );
};

export default function SessionsTable({
  loader,
  hiddenColumns = [],
  emptyInstructions,
  logoutUser,
  filter,
  isSearching,
  isPaginated,
}: SessionsTableProps) {
  const { keycloak } = useEnvironment();
  const { adminClient } = useAdminClient();

  const { realm } = useRealm();
  const { whoAmI } = useWhoAmI();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { addError } = useAlerts();
  const formatDate = useFormatDate();
  const [key, setKey] = useState(0);
  const refresh = () => setKey((value) => value + 1);
  const isOnUserPage = !!useMatch(UserRoute.path);

  const columns = useMemo(() => {
    const defaultColumns: Field<UserSessionRepresentation>[] = [
      {
        name: "username",
        displayKey: "user",
        cellRenderer: UsernameCell,
      },
      {
        name: "type",
        displayKey: "type",
      },
      {
        name: "start",
        displayKey: "started",
        cellRenderer: (row) => formatDate(new Date(row.start!)),
      },
      {
        name: "lastAccess",
        displayKey: "lastAccess",
        cellRenderer: (row) => formatDate(new Date(row.lastAccess!)),
      },
      {
        name: "ipAddress",
        displayKey: "ipAddress",
      },
      {
        name: "clients",
        displayKey: "clients",
        cellRenderer: ClientsCell,
      },
    ];

    return defaultColumns.filter(
      ({ name }) => !hiddenColumns.includes(name as ColumnName),
    );
  }, [realm, hiddenColumns]);

  const [toggleLogoutDialog, LogoutConfirm] = useConfirmDialog({
    titleKey: "logoutAllSessions",
    messageKey: "logoutAllDescription",
    continueButtonLabel: "confirm",
    onConfirm: async () => {
      try {
        await adminClient.users.logout({ id: logoutUser! });
        if (isOnUserPage && isLightweightUser(logoutUser)) {
          navigate(toUsers({ realm: realm }));
        } else {
          refresh();
        }
      } catch (error) {
        addError("logoutAllSessionsError", error);
      }
    },
  });

  async function onClickRevoke(rowData: IRowData) {
    const session = rowData.data as UserSessionRepresentation;
    await adminClient.realms.deleteSession({
      realm,
      session: session.id!,
      isOffline: true,
    });

    refresh();
  }

  async function onClickSignOut(rowData: IRowData) {
    const session = rowData.data as UserSessionRepresentation;
    await adminClient.realms.deleteSession({
      realm,
      session: session.id!,
      isOffline: false,
    });

    if (session.userId === whoAmI.userId) {
      await keycloak.logout({ redirectUri: "" });
    } else if (isOnUserPage && isLightweightUser(session.userId)) {
      navigate(toUsers({ realm: realm }));
    } else {
      refresh();
    }
  }

  return (
    <>
      <LogoutConfirm />
      <KeycloakDataTable
        key={key}
        loader={loader}
        ariaLabelKey="titleSessions"
        searchPlaceholderKey="searchForSession"
        isPaginated={isPaginated}
        isSearching={isSearching}
        searchTypeComponent={filter}
        toolbarItem={
          logoutUser && (
            <ToolbarItem>
              <Button onClick={toggleLogoutDialog}>
                {t("logoutAllSessions")}
              </Button>
            </ToolbarItem>
          )
        }
        columns={columns}
        actionResolver={(rowData: IRowData) => {
          if (
            rowData.data.type === "Offline" ||
            rowData.data.type === "OFFLINE"
          ) {
            return [
              {
                title: t("revoke"),
                onClick: () => onClickRevoke(rowData),
              } as Action<UserSessionRepresentation>,
            ];
          }
          return [
            {
              title: t("signOut"),
              onClick: () => onClickSignOut(rowData),
            } as Action<UserSessionRepresentation>,
          ];
        }}
        emptyState={
          <ListEmptyState
            hasIcon
            icon={CubesIcon}
            message={t("noSessions")}
            primaryActionText={t("refresh")}
            onPrimaryAction={refresh}
            instructions={
              emptyInstructions ? emptyInstructions : t("noSessionsDescription")
            }
          />
        }
      />
    </>
  );
}
