import { ReactElement, useState } from "react";
import {
  Button,
  ButtonVariant,
  Modal,
  ModalVariant,
  TextInput,
  FormGroup,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

export const useOffboardingDialog = (
  props: OffboardingDialogProps,
): [() => void, () => ReactElement] => {
  const [show, setShow] = useState(false);

  function toggleDialog() {
    setShow((show) => !show);
  }

  const Dialog = () => (
    <OffboardingDialogModal
      key="offboardingDialog"
      {...props}
      open={show}
      toggleDialog={toggleDialog}
    />
  );
  return [toggleDialog, Dialog];
};

export interface OffboardingDialogModalProps extends OffboardingDialogProps {
  open: boolean;
  toggleDialog: () => void;
}

export type OffboardingDialogProps = {
  titleKey: string;
  messageKey: string;
  confirmationText: string;
  onConfirm: () => void;
  onCancel?: () => void;
};

export const OffboardingDialogModal = ({
  titleKey,
  messageKey,
  confirmationText,
  onConfirm,
  onCancel,
  open = true,
  toggleDialog,
}: OffboardingDialogModalProps) => {
  const { t } = useTranslation();
  const [inputValue, setInputValue] = useState("");
  
  const isConfirmationValid = inputValue === confirmationText;

  const handleConfirm = () => {
    if (isConfirmationValid) {
      onConfirm();
      toggleDialog();
    }
  };

  const handleClose = () => {
    setInputValue("");
    toggleDialog();
  };

  return (
    <Modal
      title={t(titleKey, "Offboard Provider")}
      isOpen={open}
      onClose={handleClose}
      variant={ModalVariant.small}
      actions={[
        <Button
          id="modal-confirm"
          data-testid="confirm"
          key="confirm"
          isDisabled={!isConfirmationValid}
          variant={ButtonVariant.danger}
          onClick={handleConfirm}
        >
          {t("offboard", "Offboard")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            if (onCancel) onCancel();
            handleClose();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <div className="pf-v5-u-mb-md">
        {t(messageKey, "Are you sure you want to offboard this provider? This action cannot be undone.")}
      </div>
      <FormGroup
        label={t("offboardingConfirmationLabel", `Type "${confirmationText}" to confirm:`, { text: confirmationText })}
        fieldId="offboarding-confirmation"
        isRequired
      >
        <TextInput
          id="offboarding-confirmation"
          data-testid="offboarding-confirmation"
          value={inputValue}
          onChange={(_event, value) => setInputValue(value)}
          placeholder={confirmationText}
        />
      </FormGroup>
    </Modal>
  );
};