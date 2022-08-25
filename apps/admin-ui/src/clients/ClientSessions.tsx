import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type UserSessionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userSessionRepresentation";
import { PageSection } from "@patternfly/react-core";

import { useTranslation } from "react-i18next";

import type { LoaderFunction } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";
import SessionsTable from "../sessions/SessionsTable";

type ClientSessionsProps = {
  client: ClientRepresentation;
};

export const ClientSessions = ({ client }: ClientSessionsProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation("sessions");

  const loader: LoaderFunction<UserSessionRepresentation> = (first, max) =>
    adminClient.clients.listSessions({ id: client.id!, first, max });

  return (
    <PageSection variant="light" className="pf-u-p-0">
      <SessionsTable
        loader={loader}
        hiddenColumns={["clients"]}
        emptyInstructions={t("noSessionsForClient")}
      />
    </PageSection>
  );
};
