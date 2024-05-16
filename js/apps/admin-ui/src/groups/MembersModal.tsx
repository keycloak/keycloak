import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  AlertVariant,
  Button,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { differenceBy } from "lodash-es";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { emptyFormatter } from "../util";

type MemberModalProps = {
  groupId: string;
  onClose: () => void;
};

export const MemberModal = ({ groupId, onClose }: MemberModalProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const [selectedRows, setSelectedRows] = useState<UserRepresentation[]>([]);

  const loader = async (first?: number, max?: number, search?: string) => {
    const members = await adminClient.groups.listMembers({ id: groupId });
    const params: { [name: string]: string | number } = {
      first: first!,
      max: max! + members.length,
      search: search || "",
    };

    try {
      const users = await adminClient.users.find({ ...params });
      return differenceBy(users, members, "id").slice(0, max);
    } catch (error) {
      addError("noUsersFoundError", error);
      return [];
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
          onClick={async () => {
            try {
              await Promise.all(
                selectedRows.map((user) =>
                  adminClient.users.addToGroup({ id: user.id!, groupId }),
                ),
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
        ariaLabelKey="titleUsers"
        searchPlaceholderKey="searchForUser"
        canSelectAll
        onSelect={(rows) => setSelectedRows([...rows])}
        emptyState={
          <ListEmptyState
            message={t("noUsersFound")}
            instructions={t("emptyInstructions")}
          />
        }
        columns={[
          {
            name: "username",
            displayKey: "username",
          },
          {
            name: "email",
            displayKey: "email",
          },
          {
            name: "lastName",
            displayKey: "lastName",
            cellFormatters: [emptyFormatter()],
          },
          {
            name: "firstName",
            displayKey: "firstName",
            cellFormatters: [emptyFormatter()],
          },
        ]}
      />
    </Modal>
  );
};
