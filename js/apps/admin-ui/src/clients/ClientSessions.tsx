import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type UserSessionRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userSessionRepresentation";
import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { fetchAdminUI } from "../context/auth/admin-ui-endpoint";
import SessionsTable from "../sessions/SessionsTable";

type ClientSessionsProps = {
  client: ClientRepresentation;
};

export const ClientSessions = ({ client }: ClientSessionsProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const loader = async (first?: number, max?: number, search?: string) =>
    fetchAdminUI<UserSessionRepresentation[]>(
      adminClient,
      "ui-ext/sessions/client",
      {
        first: `${first}`,
        max: `${max}`,
        type: "ALL",
        clientId: client.id!,
        search: search || "",
      },
    );

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
