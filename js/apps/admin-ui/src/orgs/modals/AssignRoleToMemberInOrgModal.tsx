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
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import { KeycloakDataTableCustomized } from "../../components/table-toolbar/KeycloakDataTableCustomized";
import { differenceBy } from "lodash-es";

type AssignRoleToMemberProps = {
  orgId: string;
  handleModalToggle: () => void;
  refresh: () => void;
  user: UserRepresentation;
  orgRoles: RoleRepresentation[];
};

export const AssignRoleToMemberModal = ({
  handleModalToggle,
  orgId,
  user,
  orgRoles,
}: AssignRoleToMemberProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { setOrgRoleForUser, listOrgRolesForUser, revokeOrgRoleForUser } =
    useOrgFetcher(realm);
  const { addAlert, addError } = useAlerts();

  const [key, setKey] = useState(0);
  const refreshRoles = () => setKey(key + 1);

  const [selectedRows, setSelectedRows] = useState<any[]>([]);
  const [userOrgRoles, setUserOrgRoles] = useState([]);

  const saveRoles = async () => {
    const newRoles = differenceBy(selectedRows, userOrgRoles, "id");
    const rolesToRemove = differenceBy(userOrgRoles, selectedRows, "id");

    if (newRoles.length === 0 && rolesToRemove.length === 0) {
      return;
    }

    addAlert(
      `Updating roles. Granting ${newRoles.length} roles. Revoking ${rolesToRemove.length} roles.`,
    );

    try {
      await Promise.all(
        newRoles.map((newRole) => setOrgRoleForUser(orgId, newRole, user)),
      );
    } catch (e) {
      console.log("Error during assignment");
      addError("Error assigning roles.", e);
    }

    try {
      await Promise.all(
        rolesToRemove.map((roleToRemove) =>
          revokeOrgRoleForUser(orgId, roleToRemove, user),
        ),
      );
    } catch (e) {
      addError("Error removing roles.", e);
      console.log("Error during removal");
    }

    addAlert("Role assignments have been updated successfully.");
    refreshRoles();
  };

  const getListOrgRolesForUser = async () => {
    const getRolesForUser = await listOrgRolesForUser(orgId, user);

    if (getRolesForUser.error) {
      addError(
        `Error attempting to fetch available roles. Please refresh and try again.`,
        getRolesForUser,
      );

      return [];
    }

    setUserOrgRoles(getRolesForUser.data);

    return getRolesForUser.data;
  };

  const loader = async () => {
    const getRolesForUser = await getListOrgRolesForUser();
    const hasRoleIds = getRolesForUser.map((r: { id: string }) => r.id);

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
      title={`${t("assignRole")} to ${user.username || "user"}`}
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
          {t("common:save")}
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
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTableCustomized
        key={key}
        isPaginated={false}
        onSelect={(rows) => {
          return setSelectedRows([...rows]);
        }}
        // searchPlaceholderKey="clients:searchByRoleName"
        canSelectAll
        loader={loader}
        ariaLabelKey="clients:roles"
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
