import { useTranslation } from "react-i18next";
import { Modal, ModalVariant } from "@patternfly/react-core";
import {
  Table,
  TableVariant,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

type CredentialDataDialogProps = {
  credentialData: [string, string][];
  onClose: () => void;
};

export const CredentialDataDialog = ({
  credentialData,
  onClose,
}: CredentialDataDialogProps) => {
  const { t } = useTranslation();
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
      >
        <Thead>
          <Tr>
            <Th>{t("showPasswordDataName")}</Th>
            <Th>{t("showPasswordDataValue")}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {credentialData.map((cred, index) => {
            return (
              <Tr key={index}>
                <Td>{cred[0]}</Td>
                <Td>{cred[1]}</Td>
              </Tr>
            );
          })}
        </Tbody>
      </Table>
    </Modal>
  );
};
