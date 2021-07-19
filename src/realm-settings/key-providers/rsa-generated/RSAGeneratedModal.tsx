import React from "react";
import { Modal, ModalVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { RSAGeneratedForm } from "./RSAGeneratedForm";

type RSAGeneratedModalProps = {
  providerType: string;
  handleModalToggle: () => void;
  refresh: () => void;
  open: boolean;
};

export const RSAGeneratedModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh,
}: RSAGeneratedModalProps) => {
  const { t } = useTranslation("realm-settings");

  return (
    <Modal
      className="add-provider-modal"
      variant={ModalVariant.medium}
      title={t("addProvider")}
      isOpen={open}
      onClose={handleModalToggle}
    >
      <RSAGeneratedForm
        providerType={providerType}
        handleModalToggle={handleModalToggle}
        refresh={refresh}
      />
    </Modal>
  );
};
