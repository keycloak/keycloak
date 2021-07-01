import React from "react";
import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm, UseFormMethods } from "react-hook-form";

type AddMessageBundleModalProps = {
  id?: string;
  form: UseFormMethods<BundleForm>;
  save: (model: BundleForm) => void;
  handleModalToggle: () => void;
};

export type KeyValueType = { key: string; value: string };

export type BundleForm = {
  messageBundle: KeyValueType;
};

export const AddMessageBundleModal = ({
  handleModalToggle,
  save,
}: AddMessageBundleModalProps) => {
  const { t } = useTranslation("groups");
  const { register, errors, handleSubmit } = useForm();

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("realm-settings:addMessageBundle")}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid="add-bundle-confirm-button"
          key="confirm"
          variant="primary"
          type="submit"
          form="bundle-form"
        >
          {t("common:create")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form id="bundle-form" isHorizontal onSubmit={handleSubmit(save)}>
        <FormGroup
          label={t("realm-settings:key")}
          name="key"
          fieldId="email-id"
          helperTextInvalid={t("users:emailInvalid")}
          validated={
            errors.email ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <TextInput
            data-testid="key-input"
            ref={register({ required: true })}
            autoFocus
            type="text"
            id="add-key"
            name="key"
            validated={
              errors.email ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        </FormGroup>
        <FormGroup
          label={t("realm-settings:value")}
          name="add-value"
          fieldId="value-id"
          helperTextInvalid={t("users:emailInvalid")}
          validated={
            errors.email ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <TextInput
            data-testid="value-input"
            ref={register({ required: true })}
            autoFocus
            type="text"
            id="add-value"
            name="value"
            validated={
              errors.email ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
