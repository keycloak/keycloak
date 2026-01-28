import { ReactNode, useState } from "react";
import { Button, ButtonProps, Modal, ModalProps } from "@patternfly/react-core";

export type ContinueCancelModalProps = Omit<ModalProps, "ref" | "children"> & {
  modalTitle: string;
  continueLabel: string;
  cancelLabel: string;
  buttonTitle: string | ReactNode;
  buttonVariant?: ButtonProps["variant"];
  buttonTestRole?: string;
  isDisabled?: boolean;
  onContinue: () => void;
  component?: React.ElementType<any> | React.ComponentType<any>;
  children?: ReactNode;
};

export const ContinueCancelModal = ({
  modalTitle,
  continueLabel,
  cancelLabel,
  buttonTitle,
  isDisabled,
  buttonVariant,
  buttonTestRole,
  onContinue,
  component = Button,
  children,
  ...rest
}: ContinueCancelModalProps) => {
  const [open, setOpen] = useState(false);
  const Component = component;

  return (
    <>
      <Component
        variant={buttonVariant}
        onClick={() => setOpen(true)}
        isDisabled={isDisabled}
        data-testrole={buttonTestRole}
      >
        {buttonTitle}
      </Component>
      <Modal
        variant="small"
        {...rest}
        title={modalTitle}
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
            {continueLabel}
          </Button>,
          <Button
            id="modal-cancel"
            key="cancel"
            variant="secondary"
            onClick={() => setOpen(false)}
          >
            {cancelLabel}
          </Button>,
        ]}
      >
        {children}
      </Modal>
    </>
  );
};
