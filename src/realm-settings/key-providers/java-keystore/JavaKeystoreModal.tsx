import React from "react";
import { Modal, ModalVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { JavaKeystoreForm } from "./JavaKeystoreForm";

type JavaKeystoreModalProps = {
  providerType: string;
  handleModalToggle: () => void;
  refresh: () => void;
  open: boolean;
};

export const JavaKeystoreModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh,
}: JavaKeystoreModalProps) => {
  const { t } = useTranslation("realm-settings");

  return (
    <Modal
      className="add-provider-modal"
      variant={ModalVariant.medium}
      title={t("addProvider")}
      isOpen={open}
      onClose={handleModalToggle}
    >
      <JavaKeystoreForm
        providerType={providerType}
        handleModalToggle={handleModalToggle}
        refresh={refresh}
      />
    </Modal>
  );
};
