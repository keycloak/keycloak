import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import {
  AlertVariant,
  Button,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import { adminClient } from "../admin-client";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { toAddClient } from "../clients/routes/AddClient";
import useOrgFetcher from "./useOrgFetcher";
import { differenceBy } from "lodash-es";

type ClientModalProps = {
  orgId: string;
  refresh: () => void;
  onClose: () => void;
};

export const AddClient = ({ orgId, onClose, refresh }: ClientModalProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { getOrgClients, addOrgClient } = useOrgFetcher(realm);
  const { addAlert, addError } = useAlerts();
  const [selectedRows, setSelectedRows] = useState<ClientRepresentation[]>([]);

  const navigate = useNavigate();

  const goToCreate = () => navigate(toAddClient({ realm }));

  const loader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<ClientRepresentation[]> => {
    const orgClients = await getOrgClients(orgId);
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max || 100,
      search: search || "",
    };

    try {
      const clients = await adminClient.clients.find({ ...params });
      return Promise.resolve(
        differenceBy(clients as any, orgClients as any, "id").slice(
          0,
          100,
        ) as ClientRepresentation[],
      );
    } catch (error) {
      addError("groups:noClientsFoundError", error);
      return Promise.resolve([]);
    }
  };

  return (
    <Modal
      variant={ModalVariant.large}
      title={t("addClient")}
      isOpen={true}
      onClose={onClose}
      actions={[
        <Button
          data-testid="add"
          key="confirm"
          variant="primary"
          isDisabled={selectedRows.length === 0}
          onClick={async () => {
            try {
              await Promise.all(
                selectedRows.map(async (client) => {
                  await addOrgClient(orgId, client.id!);
                  refresh();
                }),
              );
              onClose();
              addAlert(
                t("clientsAdded", { count: selectedRows.length }),
                AlertVariant.success,
              );
            } catch (error) {
              addError("clientsAddedError", error);
            }
          }}
        >
          {t("add")}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant="link"
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        loader={loader}
        isPaginated
        ariaLabelKey="title"
        searchPlaceholderKey="searchForClient"
        canSelectAll
        onSelect={(rows: ClientRepresentation[]) => setSelectedRows([...rows])}
        emptyState={
          <ListEmptyState
            message={t("noClientsAvailable")}
            instructions={t("emptyInstructions")}
            primaryActionText={t("createNewClient")}
            onPrimaryAction={goToCreate}
          />
        }
        columns={[
          {
            name: "clientId",
            displayKey: "clientId",
          },
          {
            name: "name",
            displayKey: "name",
          },
        ]}
      />
    </Modal>
  );
};
