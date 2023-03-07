import { useTranslation } from "react-i18next";
import { ButtonVariant } from "@patternfly/react-core";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";

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
  const { t } = useTranslation("groups");
  const { adminClient } = useAdminClient();
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
      addError("groups:groupDeleteError", error);
    }
  };

  return (
    <ConfirmDialogModal
      titleKey={t("deleteConfirmTitle", { count: selectedRows.length })}
      messageKey={t("deleteConfirm", {
        count: selectedRows.length,
        groupName: selectedRows[0]?.name,
      })}
      continueButtonLabel="common:delete"
      continueButtonVariant={ButtonVariant.danger}
      onConfirm={multiDelete}
      open={show}
      toggleDialog={toggleDialog}
    />
  );
};
