import { useTranslation } from "react-i18next";
import {
  Modal,
  ModalBody,
  ModalHeader,
  ModalVariant,
} from "@patternfly/react-core";
import {
  Table,
  TableVariant,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

type UserAttributesDialogProps = {
  credentialScopeName: string;
  userAttributes: Record<string, string[]>;
  onClose: () => void;
};

export const UserAttributesDialog = ({
  credentialScopeName,
  userAttributes,
  onClose,
}: UserAttributesDialogProps) => {
  const { t } = useTranslation();

  const modalTitle = t("credentialUserAttributesFor", { credentialScopeName });

  return (
    <Modal
      variant={ModalVariant.medium}
      isOpen
      onClose={onClose}
      aria-label={modalTitle}
    >
      <ModalHeader title={modalTitle} />
      <ModalBody>
        <Table
          aria-label={t("credentialUserAttributes")}
          variant={TableVariant.compact}
        >
          <Thead>
            <Tr>
              <Th>{t("credentialAttributeName")}</Th>
              <Th>{t("credentialAttributeValue")}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {Object.entries(userAttributes).map(([key, values]) => (
              <Tr key={key}>
                <Td>{key}</Td>
                <Td>{values.join(", ")}</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </ModalBody>
    </Modal>
  );
};
