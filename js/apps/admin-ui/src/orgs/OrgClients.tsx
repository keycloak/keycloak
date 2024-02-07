import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import {
  Action,
  KeycloakDataTable,
} from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { useRealm } from "../context/realm-context/RealmContext";
import { AddClient } from "./AddClient";
import type { OrgRepresentation } from "./routes";
import useOrgFetcher from "./useOrgFetcher";
import { Button, ToolbarItem } from "@patternfly/react-core";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { Link } from "react-router-dom";
import { toClient } from "../clients/routes/Client";
import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { AssignRoleToClientModal } from "./modals/AssignRoleToClientInOrgModal";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";

type OrgClientsTypeProps = {
  org: OrgRepresentation;
};

type ClientsOf = ClientRepresentation & {
  client: GroupRepresentation[];
};

export default function OrgClients({ org }: OrgClientsTypeProps) {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());
  const { getOrgClients, removeClientFromOrg, getRolesForOrg } =
    useOrgFetcher(realm);
  const [assignRoleModalOpen, setAssignRoleModalOpen] = useState<
    ClientRepresentation | boolean
  >(false);

  const [orgRoles, setOrgRoles] = useState<RoleRepresentation[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      const data = await getRolesForOrg(org.id);
      setOrgRoles(data);
    };

    fetchData();
  }, []);

  const loader = async (): Promise<ClientsOf[]> => {
    return await getOrgClients(org.id);
  };

  const ClientDetailLink = (client: ClientsOf) => (
    <Link
      key={client.id}
      to={toClient({ realm, clientId: client.id!, tab: "settings" })}
    >
      {client.clientId}
    </Link>
  );

  const [addClientsVisibility, setAddClientsVisibility] = useState(false);
  const toggleAddClientsVisibility = () =>
    setAddClientsVisibility(!addClientsVisibility);

  return (
    <>
      {addClientsVisibility && (
        <AddClient
          refresh={refresh}
          orgId={org.id}
          onClose={toggleAddClientsVisibility}
        />
      )}
      {assignRoleModalOpen && (
        <AssignRoleToClientModal
          orgId={org.id}
          client={assignRoleModalOpen as ClientRepresentation}
          handleModalToggle={() => setAssignRoleModalOpen(false)}
          refresh={refresh}
          orgRoles={orgRoles}
        />
      )}
      <KeycloakDataTable
        data-testid="clients-org-table"
        key={key}
        loader={loader}
        ariaLabelKey="clients"
        toolbarItem={
          <ToolbarItem>
            <Button
              data-testid="addClient"
              variant="primary"
              onClick={toggleAddClientsVisibility}
            >
              Add Client
            </Button>
          </ToolbarItem>
        }
        actions={[
          {
            title: "Remove from Org",
            onRowClick: async (
              client: ClientRepresentation,
            ): Promise<boolean> => {
              await removeClientFromOrg(org.id, client.id!);
              refresh();
              return Promise.resolve(true);
            },
          } as Action<ClientRepresentation>,
        ]}
        columns={[
          {
            name: "clientId",
            displayKey: "ClientId",
            cellRenderer: ClientDetailLink,
          },
          {
            name: "name",
            displayKey: "Name",
          },
          {
            name: "rootUrl",
            displayKey: "URL",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("noClientsFound")}
            instructions={t("emptyInstructions")}
            primaryActionText={t("addClient")}
            onPrimaryAction={toggleAddClientsVisibility}
          />
        }
      />
    </>
  );
}
