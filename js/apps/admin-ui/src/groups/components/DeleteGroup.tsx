import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { ButtonVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";

type DeleteConfirmProps = {
  selectedRows: GroupRepresentation[];
  show: boolean;
  toggleDialog: () => void;
  refresh: () => void;
};

export const DeleteGroup = ({
  selectedRows,
  show,
  toggleDialog,
  refresh,
}: DeleteConfirmProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const multiDelete = async () => {
    try {
      for (const group of selectedRows) {
        await adminClient.groups.del({
          id: group.id!,
        });
      }
      refresh();
      addAlert(t("groupDeleted", { count: selectedRows.length }));
    } catch (error) {
      addError("groupDeleteError", error);
    }
  };

  return (
    <ConfirmDialogModal
      titleKey={t("deleteConfirmTitle", { count: selectedRows.length })}
      messageKey={t("deleteConfirmGroup", {
        count: selectedRows.length,
        groupName: selectedRows[0]?.name,
      })}
      continueButtonLabel="delete"
      continueButtonVariant={ButtonVariant.danger}
      onConfirm={multiDelete}
      open={show}
      toggleDialog={toggleDialog}
    />
  );
};
