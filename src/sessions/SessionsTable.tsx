import type UserSessionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userSessionRepresentation";
import { List, ListItem, ListVariant } from "@patternfly/react-core";
import { CubesIcon } from "@patternfly/react-icons";
import React, { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { toClient } from "../clients/routes/Client";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import {
  Field,
  KeycloakDataTable,
  LoaderFunction,
} from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { toUser } from "../user/routes/User";
import { dateFormatter } from "../util";

export type ColumnName = "username" | "start" | "lastAccess" | "clients";

export type SessionsTableProps = {
  loader: LoaderFunction<UserSessionRepresentation>;
  hiddenColumns?: ColumnName[];
  emptyInstructions?: string;
};

export default function SessionsTable({
  loader,
  hiddenColumns = [],
  emptyInstructions,
}: SessionsTableProps) {
  const { realm } = useRealm();
  const { whoAmI } = useWhoAmI();
  const { t } = useTranslation("sessions");
  const adminClient = useAdminClient();
  const [key, setKey] = useState(0);
  const locale = whoAmI.getLocale();
  const refresh = () => setKey((value) => value + 1);

  const columns = useMemo(() => {
    const UsernameCell = (row: UserSessionRepresentation) => (
      <Link to={toUser({ realm, id: row.userId!, tab: "sessions" })}>
        {row.username}
      </Link>
    );

    const ClientsCell = (row: UserSessionRepresentation) => (
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

    const defaultColumns: Field<UserSessionRepresentation>[] = [
      {
        name: "username",
        displayKey: "sessions:user",
        cellRenderer: UsernameCell,
      },
      {
        name: "start",
        displayKey: "sessions:started",
        cellFormatters: [dateFormatter(locale)],
      },
      {
        name: "lastAccess",
        displayKey: "sessions:lastAccess",
        cellFormatters: [dateFormatter(locale)],
      },
      {
        name: "clients",
        displayKey: "sessions:clients",
        cellRenderer: ClientsCell,
      },
    ];

    return defaultColumns.filter(
      ({ name }) => !hiddenColumns.includes(name as ColumnName)
    );
  }, [realm, locale, hiddenColumns]);

  async function onClickSignOut(session: UserSessionRepresentation) {
    await adminClient.realms.deleteSession({ realm, session: session.id! });

    if (session.userId === whoAmI.getUserId()) {
      await adminClient.keycloak?.logout({ redirectUri: "" });
    } else {
      refresh();
    }
  }

  return (
    <KeycloakDataTable
      key={key}
      loader={loader}
      ariaLabelKey="sessions:title"
      searchPlaceholderKey="sessions:searchForSession"
      columns={columns}
      actions={[
        {
          title: t("common:signOut"),
          onRowClick: onClickSignOut,
        },
      ]}
      emptyState={
        <ListEmptyState
          hasIcon
          icon={CubesIcon}
          message={t("noSessions")}
          instructions={
            emptyInstructions ? emptyInstructions : t("noSessionsDescription")
          }
        />
      }
    />
  );
}
