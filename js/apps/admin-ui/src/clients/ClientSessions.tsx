import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type UserSessionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userSessionRepresentation";
import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import type { LoaderFunction } from "../components/table-toolbar/KeycloakDataTable";
import SessionsTable from "../sessions/SessionsTable";

type ClientSessionsProps = {
  client: ClientRepresentation;
};

export const ClientSessions = ({ client }: ClientSessionsProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const loader: LoaderFunction<UserSessionRepresentation> = async (
    first,
    max,
  ) => {
    const mapSessionsToType =
      (type: string) => (sessions: UserSessionRepresentation[]) =>
        sessions.map((session) => ({
          type,
          ...session,
        }));

    const allSessions = await Promise.all([
      adminClient.clients
        .listSessions({ id: client.id!, first, max })
        .then(mapSessionsToType(t("sessionsType.regularSSO"))),
      adminClient.clients
        .listOfflineSessions({
          id: client.id!,
          first,
          max,
        })
        .then(mapSessionsToType(t("sessionsType.offline"))),
    ]);

    return allSessions.flat();
  };

  return (
    <PageSection variant="light" className="pf-v5-u-p-0">
      <SessionsTable
        loader={loader}
        hiddenColumns={["clients"]}
        emptyInstructions={t("noSessionsForClient")}
      />
    </PageSection>
  );
};
