import React from "react";
import { Modal, ModalVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { RSAForm } from "./RSAForm";

type RSAModalProps = {
  providerType: string;
  handleModalToggle: () => void;
  refresh: () => void;
  open: boolean;
};

export const RSAModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh,
}: RSAModalProps) => {
  const { t } = useTranslation("realm-settings");

  return (
    <Modal
      className="add-provider-modal"
      variant={ModalVariant.medium}
      title={t("addProvider")}
      isOpen={open}
      onClose={handleModalToggle}
    >
      <RSAForm
        providerType={providerType}
        handleModalToggle={handleModalToggle}
        refresh={refresh}
      />
    </Modal>
  );
};
