import {
  Alert,
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Switch,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { saveAs } from "file-saver";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { prettyPrintJSON } from "../util";

import "./partial-export.css";

export type PartialExportDialogProps = {
  isOpen: boolean;
  onClose: () => void;
};

export const PartialExportDialog = ({
  isOpen,
  onClose,
}: PartialExportDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { realm } = useRealm();
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

      saveAs(
        new Blob([prettyPrintJSON(realmExport)], {
          type: "application/json",
        }),
        "realm-export.json",
      );

      addAlert(t("exportSuccess"), AlertVariant.success);
      onClose();
    } catch (error) {
      addError("exportFail", error);
    }

    setIsExporting(false);
  }

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("partialExport")}
      isOpen={isOpen}
      onClose={onClose}
      actions={[
        <Button
          key="export"
          data-testid="confirm"
          isDisabled={isExporting}
          onClick={exportRealm}
        >
          {t("export")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <TextContent>
        <Text>{t("partialExportHeaderText")}</Text>
      </TextContent>
      <Form
        isHorizontal
        className="keycloak__realm-settings__partial-import_form"
      >
        <FormGroup
          label={t("includeGroupsAndRoles")}
          fieldId="include-groups-and-roles-check"
          hasNoPaddingTop
        >
          <Switch
            id="include-groups-and-roles-check"
            data-testid="include-groups-and-roles-check"
            isChecked={exportGroupsAndRoles}
            onChange={(_event, val) => setExportGroupsAndRoles(val)}
            label={t("on")}
            labelOff={t("off")}
            aria-label={t("includeGroupsAndRoles")}
          />
        </FormGroup>
        <FormGroup
          label={t("includeClients")}
          fieldId="include-clients-check"
          hasNoPaddingTop
        >
          <Switch
            id="include-clients-check"
            data-testid="include-clients-check"
            onChange={(_event, val) => setExportClients(val)}
            isChecked={exportClients}
            label={t("on")}
            labelOff={t("off")}
            aria-label={t("includeClients")}
          />
        </FormGroup>
      </Form>

      {showWarning && (
        <Alert
          data-testid="warning-message"
          variant="warning"
          component="p"
          title={t("exportWarningTitle")}
          isInline
        >
          {t("exportWarningDescription")}
        </Alert>
      )}
    </Modal>
  );
};
