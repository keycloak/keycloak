import React from "react";
import { Modal, ModalVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HMACGeneratedForm } from "./HMACGeneratedForm";

type HMACGeneratedModalProps = {
  providerType: string;
  handleModalToggle: () => void;
  refresh: () => void;
  open: boolean;
};

export const HMACGeneratedModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh,
}: HMACGeneratedModalProps) => {
  const { t } = useTranslation("realm-settings");

  return (
    <Modal
      className="add-provider-modal"
      variant={ModalVariant.medium}
      title={t("addProvider")}
      isOpen={open}
      onClose={handleModalToggle}
    >
      <HMACGeneratedForm
        providerType={providerType}
        handleModalToggle={handleModalToggle}
        refresh={refresh}
      />
    </Modal>
  );
};
