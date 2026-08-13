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
  title: string;
  credentialData: [string, string][];
  onClose: () => void;
};

export const CredentialDataDialog = ({
  title,
  credentialData,
  onClose,
}: CredentialDataDialogProps) => {
  const { t } = useTranslation();
  return (
    <Modal
      variant={ModalVariant.medium}
      title={title}
      data-testid="passwordDataDialog"
      isOpen
      onClose={onClose}
    >
      <Table
        aria-label={title}
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
