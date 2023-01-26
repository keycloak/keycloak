// @ts-nocheck
import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  ValidatedOptions,
} from "@patternfly/react-core";
import { SubmitHandler, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";

import type { KeyValueType } from "../components/key-value-form/key-value-convert";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";

type AddMessageBundleModalProps = {
  id?: string;
  form: UseFormReturn<BundleForm>;
  save: SubmitHandler<BundleForm>;
  handleModalToggle: () => void;
};

export type BundleForm = {
  messageBundle: KeyValueType;
};

export const AddMessageBundleModal = ({
  handleModalToggle,
  save,
  form: {
    register,
    handleSubmit,
    formState: { errors },
  },
}: AddMessageBundleModalProps) => {
  const { t } = useTranslation("realm-settings");

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("addMessageBundle")}
      isOpen
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
          data-testid="cancel"
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
          label={t("common:key")}
          name="key"
          fieldId="key-id"
          helperTextInvalid={t("common:required")}
          validated={
            errors.key ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <KeycloakTextInput
            data-testid="key-input"
            autoFocus
            id="key-id"
            validated={
              errors.key ? ValidatedOptions.error : ValidatedOptions.default
            }
            {...register("key", { required: true })}
          />
        </FormGroup>
        <FormGroup
          label={t("common:value")}
          name="add-value"
          fieldId="value-id"
          helperTextInvalid={t("common:required")}
          validated={
            errors.value ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <KeycloakTextInput
            data-testid="value-input"
            id="value-id"
            validated={
              errors.value ? ValidatedOptions.error : ValidatedOptions.default
            }
            {...register("value", { required: true })}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
