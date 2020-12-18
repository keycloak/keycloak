import React from "react";
import { Link } from "react-router-dom";
import { PageSection } from "@patternfly/react-core";
import moment from "moment";
import UserSessionRepresentation from "keycloak-admin/lib/defs/userSessionRepresentation";

import { ViewHeader } from "../components/view-header/ViewHeader";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";

const Clients = (row: UserSessionRepresentation) => {
  return (
    <>
      {Object.values(row.clients!).map((client) => (
        <Link key={client} to="" className="pf-u-mx-sm">
          {client}
        </Link>
      ))}
    </>
  );
};

export const SessionsSection = () => {
  const adminClient = useAdminClient();

  const loader = async () => {
    const activeClients = await adminClient.sessions.find();
    const clientSessions = (
      await Promise.all(
        activeClients.map((client) =>
          adminClient.clients.listSessions({ id: client.id })
        )
      )
    ).flat();

    const userSessions = (
      await Promise.all(
        clientSessions.map((session) =>
          adminClient.users.listSessions({ id: session.userId! })
        )
      )
    ).flat();

    return userSessions;
  };

  return (
    <>
      <ViewHeader titleKey="sessions:title" subKey="sessions:sessionExplain" />
      <PageSection variant="light">
        <KeycloakDataTable
          loader={loader}
          ariaLabelKey="session:title"
          searchPlaceholderKey="sessions:searchForSession"
          columns={[
            {
              name: "username",
              displayKey: "sessions:subject",
            },
            {
              name: "lastAccess",
              displayKey: "sessions:lastAccess",
              cellRenderer: (row) => moment(row.lastAccess).fromNow(),
            },
            {
              name: "start",
              displayKey: "sessions:startDate",
              cellRenderer: (row) => moment(row.lastAccess).format("LLL"),
            },
            {
              name: "clients",
              displayKey: "sessions:accessedClients",
              cellRenderer: Clients,
            },
          ]}
        />
      </PageSection>
    </>
  );
};
