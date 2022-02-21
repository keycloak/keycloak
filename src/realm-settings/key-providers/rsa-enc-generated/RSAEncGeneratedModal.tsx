import React from "react";
import { Modal, ModalVariant } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { RSAGeneratedForm } from "../rsa-generated/RSAGeneratedForm";

type RSAEncGeneratedModalProps = {
  providerType: string;
  handleModalToggle: () => void;
  refresh: () => void;
  open: boolean;
};

export const RSAEncGeneratedModal = ({
  providerType,
  handleModalToggle,
  open,
  refresh,
}: RSAEncGeneratedModalProps) => {
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
        isRSAEncGenerated
        providerType={providerType}
        handleModalToggle={handleModalToggle}
        refresh={refresh}
      />
    </Modal>
  );
};
