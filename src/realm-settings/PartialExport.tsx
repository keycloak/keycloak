import {
  Alert,
  AlertVariant,
  Button,
  ButtonVariant,
  Checkbox,
  Modal,
  ModalVariant,
  Stack,
  StackItem,
  Text,
  TextContent,
} from "@patternfly/react-core";
import FileSaver from "file-saver";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAlerts } from "../components/alert/Alerts";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { prettyPrintJSON } from "../util";

export type PartialExportDialogProps = {
  isOpen: boolean;
  onClose: () => void;
};

export const PartialExportDialog = ({
  isOpen,
  onClose,
}: PartialExportDialogProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const [exportGroupsAndRoles, setExportGroupsAndRoles] = useState(false);
  const [exportClients, setExportClients] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  const showWarning = exportGroupsAndRoles || exportClients;

  async function exportRealm() {
    setIsExporting(true);

    try {
      const realmExport = await adminClient.realms.export({
        realm,
        exportClients,
        exportGroupsAndRoles,
      });

      FileSaver.saveAs(
        new Blob([prettyPrintJSON(realmExport)], {
          type: "application/json",
        }),
        "realm-export.json"
      );

      addAlert(t("partial-export:exportSuccess"), AlertVariant.success);
      onClose();
    } catch (error) {
      addError("partial-export:exportFail", error);
    }

    setIsExporting(false);
  }

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("realm-settings:partialExport")}
      isOpen={isOpen}
      onClose={onClose}
      actions={[
        <Button
          key="export"
          data-testid="export-button"
          isDisabled={isExporting}
          onClick={exportRealm}
        >
          {t("common:export")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancel-button"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Stack hasGutter>
        <StackItem>
          <TextContent>
            <Text>{t("partial-export:partialExportHeaderText")}</Text>
          </TextContent>
        </StackItem>
        <StackItem>
          <Checkbox
            id="include-groups-and-roles-check"
            label={t("partial-export:includeGroupsAndRoles")}
            isChecked={exportGroupsAndRoles}
            onChange={setExportGroupsAndRoles}
          />
        </StackItem>
        <StackItem>
          <Checkbox
            id="include-clients-check"
            label={t("partial-export:includeClients")}
            isChecked={exportClients}
            onChange={setExportClients}
          />
        </StackItem>
        {showWarning && (
          <StackItem>
            <Alert
              data-testid="warning-message"
              variant="warning"
              title={t("partial-export:exportWarningTitle")}
              isInline
            >
              {t("partial-export:exportWarningDescription")}
            </Alert>
          </StackItem>
        )}
      </Stack>
    </Modal>
  );
};
