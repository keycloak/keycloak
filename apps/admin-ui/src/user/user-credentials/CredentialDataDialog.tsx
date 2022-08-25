import { useTranslation } from "react-i18next";
import { Modal, ModalVariant } from "@patternfly/react-core";
import {
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";

type CredentialDataDialogProps = {
  credentialData: [string, string][];
  onClose: () => void;
};

export const CredentialDataDialog = ({
  credentialData,
  onClose,
}: CredentialDataDialogProps) => {
  const { t } = useTranslation("users");
  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("passwordDataTitle")}
      data-testid="passwordDataDialog"
      isOpen
      onClose={onClose}
    >
      <Table
        aria-label={t("passwordDataTitle")}
        data-testid="password-data-dialog"
        variant={TableVariant.compact}
        cells={[t("showPasswordDataName"), t("showPasswordDataValue")]}
        rows={credentialData}
      >
        <TableHeader />
        <TableBody />
      </Table>
    </Modal>
  );
};
