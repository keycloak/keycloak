import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import {
  AlertVariant,
  Button,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { adminClient } from "../admin-client";
import { useRealm } from "../context/realm-context/RealmContext";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { emptyFormatter } from "../util";
import { toAddUser } from "../user/routes/AddUser";
import useOrgFetcher from "./useOrgFetcher";
import { differenceBy } from "lodash-es";

type MemberModalProps = {
  orgId: string;
  refresh: () => void;
  onClose: () => void;
};

export const AddMember = ({ orgId, onClose, refresh }: MemberModalProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { getOrgMembers, addOrgMember } = useOrgFetcher(realm);
  const { addAlert, addError } = useAlerts();
  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);

  const navigate = useNavigate();

  const goToCreate = () => navigate(toAddUser({ realm }));

  const loader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<UserRepresentation[]> => {
    const members = await getOrgMembers(orgId);
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max || 100,
      search: search || "",
    };

    try {
      const users = await adminClient.users.find({ ...params });
      return Promise.resolve(
        differenceBy(users as any, members as any, "id").slice(
          0,
          100,
        ) as UserRepresentation[],
      );
    } catch (error) {
      addError("groups:noUsersFoundError", error);
      return Promise.resolve([]);
    }
  };

  return (
    <Modal
      variant={ModalVariant.large}
      title={t("addMember")}
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
                selectedRows.map(async (user) => {
                  await addOrgMember(orgId, user.id!);
                  refresh();
                }),
              );
              onClose();
              addAlert(
                t("usersAdded", { count: selectedRows.length }),
                AlertVariant.success,
              );
            } catch (error) {
              addError("usersAddedError", error);
            }
          }}
        >
          {t("common:add")}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant="link"
          onClick={onClose}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        loader={loader}
        isPaginated
        ariaLabelKey="users:title"
        searchPlaceholderKey="users:searchForUser"
        canSelectAll
        onSelect={(rows: UserRepresentation[]) => setSelectedRows([...rows])}
        emptyState={
          <ListEmptyState
            message={t("users:noUsersAvailable")}
            instructions={t("users:emptyInstructions")}
            primaryActionText={t("users:createNewUser")}
            onPrimaryAction={goToCreate}
          />
        }
        columns={[
          {
            name: "username",
            displayKey: "users:username",
          },
          {
            name: "email",
            displayKey: "users:email",
          },
          {
            name: "lastName",
            displayKey: "users:lastName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "firstName",
            displayKey: "users:firstName",
            cellFormatters: [emptyFormatter()],
          },
        ]}
      />
    </Modal>
  );
};
