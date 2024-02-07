import { useState } from "react";
import {
  Button,
  ButtonVariant,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAlerts } from "../../components/alert/Alerts";
import useOrgFetcher from "../useOrgFetcher";
import { useRealm } from "../../context/realm-context/RealmContext";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { KeycloakDataTableCustomized } from "../../components/table-toolbar/KeycloakDataTableCustomized";
import { differenceBy } from "lodash-es";

type AssignRoleToClientProps = {
  orgId: string;
  handleModalToggle: () => void;
  refresh: () => void;
  client: ClientRepresentation;
  orgRoles: RoleRepresentation[];
};

export const AssignRoleToClientModal = ({
  handleModalToggle,
  orgId,
  client,
  orgRoles,
}: AssignRoleToClientProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { setOrgRoleForClient, listOrgRolesForClient, revokeOrgRoleForClient } =
    useOrgFetcher(realm);
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refreshRoles = () => setKey(key + 1);

  const [selectedRows, setSelectedRows] = useState<any[]>([]);
  const [clientOrgRoles, setClientOrgRoles] = useState([]);

  const saveRoles = async () => {
    const newRoles = differenceBy(selectedRows, clientOrgRoles, "id");
    const rolesToRemove = differenceBy(clientOrgRoles, selectedRows, "id");

    if (newRoles.length === 0 && rolesToRemove.length === 0) {
      return;
    }

    addAlert(
      `Updating roles. Granting ${newRoles.length} roles. Revoking ${rolesToRemove.length} roles.`,
    );

    try {
      await Promise.all(
        newRoles.map((newRole) => setOrgRoleForClient(orgId, newRole, client)),
      );
    } catch (e) {
      console.log("Error during assignment");
      addError("Error assigning roles.", e);
    }

    try {
      await Promise.all(
        rolesToRemove.map((roleToRemove) =>
          revokeOrgRoleForClient(orgId, roleToRemove, client),
        ),
      );
    } catch (e) {
      addError("Error removing roles.", e);
      console.log("Error during removal");
    }

    addAlert("Role assignments have been updated successfully.");
    refreshRoles();
  };

  const getListOrgRolesForClient = async () => {
    const getRolesForClient = await listOrgRolesForClient(orgId, client);

    if (getRolesForClient.error) {
      addError(
        `Error attempting to fetch available roles. Please refresh and try again.`,
        getRolesForClient,
      );

      return [];
    }

    setClientOrgRoles(getRolesForClient.data);

    return getRolesForClient.data;
  };

  const loader = async () => {
    const getRolesForClient = await getListOrgRolesForClient();
    const hasRoleIds = getRolesForClient.map((r: { id: string }) => r.id);

    let tSelected: any[] = [];
    const roleMap = orgRoles.map((orgRole) => {
      const isSelected = hasRoleIds.includes(orgRole.id);
      const updatedRole = {
        ...orgRole,
        isSelected,
      };
      if (isSelected) {
        tSelected = [...tSelected, updatedRole];
      }
      return {
        ...orgRole,
        isSelected,
      };
    });

    setSelectedRows(tSelected);
    return roleMap;
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      title={`${t("assignRole")} to ${client.clientId || "client"}`}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid={`assignRole`}
          key="confirm"
          variant="primary"
          type="submit"
          onClick={saveRoles}
        >
          {t("save")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTableCustomized
        key={key}
        isPaginated={false}
        onSelect={(rows) => {
          return setSelectedRows([...rows]);
        }}
        // searchPlaceholderKey="searchByRoleName"
        canSelectAll
        loader={loader}
        ariaLabelKey="roles"
        columns={[
          {
            name: "name",
            // cellRenderer: ServiceRole,
          },
          {
            name: "description",
            displayKey: t("description"),
          },
        ]}
      />
    </Modal>
  );
};
