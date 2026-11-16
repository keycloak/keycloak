import {
  Modal,
  ModalBody,
  ModalHeader,
  ModalVariant,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { KeyProviderForm } from "./KeyProviderForm";
import type { ProviderType } from "../../routes/KeyProvider";

import style from "./key-provider-modal.module.css";

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
      className={style.dialog}
      variant={ModalVariant.medium}
      isOpen
      onClose={onClose}
      aria-label={t("addProvider")}
    >
      <ModalHeader title={t("addProvider")} />
      <ModalBody>
        <KeyProviderForm providerType={providerType} onClose={onClose} />
      </ModalBody>
    </Modal>
  );
};
