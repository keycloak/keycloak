import { Modal, ModalVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { KeyProviderForm } from "./KeyProviderForm";
import type { ProviderType } from "../../routes/KeyProvider";

type KeyProviderModalProps = {
  providerType: ProviderType;
  onClose: () => void;
};

export const KeyProviderModal = ({
  providerType,
  onClose,
}: KeyProviderModalProps) => {
  const { t } = useTranslation();
  return (
    <Modal
      className="add-provider-modal"
      variant={ModalVariant.medium}
      title={t("addProvider")}
      isOpen
      onClose={onClose}
    >
      <KeyProviderForm providerType={providerType} onClose={onClose} />
    </Modal>
  );
};
