import { Button, Form, Modal, ModalVariant } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { KeyValueInput } from "../components/key-value-form/KeyValueInput";
import {
  KeyValueType,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";

type InvitationAttributesModalProps = {
  onSubmit: (attributes: Record<string, string[]>) => Promise<void>;
  onClose: () => void;
};

type InvitationAttributesForm = {
  attributes: KeyValueType[];
};

export const InvitationAttributesModal = ({
  onSubmit,
  onClose,
}: InvitationAttributesModalProps) => {
  const { t } = useTranslation();
  const form = useForm<InvitationAttributesForm>();
  const { handleSubmit } = form;

  const submitForm = async (data: InvitationAttributesForm) => {
    const attributes = keyValueToArray(data.attributes);
    await onSubmit(attributes);
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("invitationAttributes")}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="send"
          key="confirm"
          variant="primary"
          onClick={handleSubmit(submitForm)}
        >
          {t("send")}
        </Button>,
        <Button
          data-testid="cancel"
          key="cancel"
          variant="link"
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form id="invitation-attributes-form">
          <KeyValueInput name="attributes" />
        </Form>
      </FormProvider>
    </Modal>
  );
};
