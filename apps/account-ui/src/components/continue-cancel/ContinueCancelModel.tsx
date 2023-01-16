import { ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, ButtonProps, Modal, ModalProps } from "@patternfly/react-core";
import { TranslationKeys } from "../../react-i18next";

type ContinueCancelModalProps = Omit<ModalProps, "ref" | "children"> & {
  modalTitle: TranslationKeys;
  modalMessage?: string;
  buttonTitle: TranslationKeys | ReactNode;
  buttonVariant?: ButtonProps["variant"];
  isDisabled?: boolean;
  onContinue: () => void;
  continueLabel?: TranslationKeys;
  cancelLabel?: TranslationKeys;
  component?: React.ElementType<any> | React.ComponentType<any>;
  children?: ReactNode;
};

export const ContinueCancelModal = ({
  modalTitle,
  modalMessage,
  buttonTitle,
  isDisabled,
  buttonVariant,
  onContinue,
  continueLabel = "continue",
  cancelLabel = "doCancel",
  component = "button",
  children,
  ...rest
}: ContinueCancelModalProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const Component = component;

  return (
    <>
      <Component
        variant={buttonVariant}
        onClick={() => setOpen(true)}
        isDisabled={isDisabled}
      >
        {
          //@ts-ignore
          typeof buttonTitle === "string" ? t(buttonTitle) : buttonTitle
        }
      </Component>
      <Modal
        variant="small"
        {...rest}
        //@ts-ignore
        title={t(modalTitle)}
        isOpen={open}
        onClose={() => setOpen(false)}
        actions={[
          <Button
            id="modal-confirm"
            key="confirm"
            variant="primary"
            onClick={() => {
              setOpen(false);
              onContinue();
            }}
          >
            {
              //@ts-ignore
              t(continueLabel)
            }
          </Button>,
          <Button
            id="modal-cancel"
            key="cancel"
            variant="secondary"
            onClick={() => setOpen(false)}
          >
            {
              //@ts-ignore
              t(cancelLabel)
            }
          </Button>,
        ]}
      >
        {
          //@ts-ignore
          modalMessage ? t(modalMessage) : children
        }
      </Modal>
    </>
  );
};
