import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  AlertVariant,
  DropdownItem,
  DropdownSeparator,
} from "@patternfly/react-core";

import { useAlerts } from "../../components/alert/Alerts";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { useAdminClient } from "../../context/auth/AdminClient";
import { Header } from "./Header";
import { useFormContext, useWatch } from "react-hook-form";

type ExtendedHeaderProps = {
  provider: string;
  editMode?: string | string[];
  save: () => void;
  noDivider?: boolean;
};

export const ExtendedHeader = ({
  provider,
  editMode,
  save,
  noDivider = false,
}: ExtendedHeaderProps) => {
  const { t } = useTranslation("user-federation");
  const { id } = useParams<{ id: string }>();
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const { control } = useFormContext();
  const hasImportUsers = useWatch({
    name: "config.importEnabled",
    control,
    defaultValue: ["true"],
  })[0];

  const [toggleUnlinkUsersDialog, UnlinkUsersDialog] = useConfirmDialog({
    titleKey: "user-federation:userFedUnlinkUsersConfirmTitle",
    messageKey: "user-federation:userFedUnlinkUsersConfirm",
    continueButtonLabel: "user-federation:unlinkUsers",
    onConfirm: () => unlinkUsers(),
  });

  const [toggleRemoveUsersDialog, RemoveUsersConfirm] = useConfirmDialog({
    titleKey: t("removeImportedUsers"),
    messageKey: t("removeImportedUsersMessage"),
    continueButtonLabel: "common:remove",
    onConfirm: async () => {
      try {
        removeImportedUsers();
        addAlert(t("removeImportedUsersSuccess"), AlertVariant.success);
      } catch (error) {
        addError("user-federation:removeImportedUsersError", error);
      }
    },
  });

  const removeImportedUsers = async () => {
    try {
      if (id) {
        await adminClient.userStorageProvider.removeImportedUsers({ id });
      }
      addAlert(t("removeImportedUsersSuccess"), AlertVariant.success);
    } catch (error) {
      addError("user-federation:removeImportedUsersError", error);
    }
  };

  const syncChangedUsers = async () => {
    try {
      if (id) {
        const response = await adminClient.userStorageProvider.sync({
          id: id,
          action: "triggerChangedUsersSync",
        });
        if (response.ignored) {
          addAlert(`${response.status}.`, AlertVariant.warning);
        } else {
          addAlert(
            t("syncUsersSuccess") +
              `${response.added} users added, ${response.updated} users updated, ${response.removed} users removed, ${response.failed} users failed.`,
            AlertVariant.success
          );
        }
      }
    } catch (error) {
      addError("user-federation:syncUsersError", error);
    }
  };

  const syncAllUsers = async () => {
    try {
      if (id) {
        const response = await adminClient.userStorageProvider.sync({
          id: id,
          action: "triggerFullSync",
        });
        if (response.ignored) {
          addAlert(`${response.status}.`, AlertVariant.warning);
        } else {
          addAlert(
            t("syncUsersSuccess") +
              `${response.added} users added, ${response.updated} users updated, ${response.removed} users removed, ${response.failed} users failed.`,
            AlertVariant.success
          );
        }
      }
    } catch (error) {
      addError("user-federation:syncUsersError", error);
    }
  };

  const unlinkUsers = async () => {
    try {
      if (id) {
        await adminClient.userStorageProvider.unlinkUsers({ id });
      }
      addAlert(t("unlinkUsersSuccess"), AlertVariant.success);
    } catch (error) {
      addError("user-federation:unlinkUsersError", error);
    }
  };

  return (
    <>
      <UnlinkUsersDialog />
      <RemoveUsersConfirm />
      <Header
        provider={provider}
        noDivider={noDivider}
        save={save}
        dropdownItems={[
          <DropdownItem
            key="sync"
            onClick={syncChangedUsers}
            isDisabled={hasImportUsers === "false"}
          >
            {t("syncChangedUsers")}
          </DropdownItem>,
          <DropdownItem
            key="syncall"
            onClick={syncAllUsers}
            isDisabled={hasImportUsers === "false"}
          >
            {t("syncAllUsers")}
          </DropdownItem>,
          <DropdownItem
            key="unlink"
            isDisabled={editMode ? !editMode.includes("UNSYNCED") : false}
            onClick={toggleUnlinkUsersDialog}
          >
            {t("unlinkUsers")}
          </DropdownItem>,
          <DropdownItem key="remove" onClick={toggleRemoveUsersDialog}>
            {t("removeImported")}
          </DropdownItem>,
          <DropdownSeparator key="separator" />,
        ]}
      />
    </>
  );
};
