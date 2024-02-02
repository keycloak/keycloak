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

type AddTranslationModalProps = {
  id?: string;
  form: UseFormReturn<TranslationForm>;
  save: SubmitHandler<TranslationForm>;
  handleModalToggle: () => void;
};

export type TranslationForm = {
  key: string;
  value: string;
  translation: KeyValueType;
};

export const AddTranslationModal = ({
  handleModalToggle,
  save,
  form: {
    register,
    handleSubmit,
    formState: { errors },
  },
}: AddTranslationModalProps) => {
  const { t } = useTranslation();

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("AddTranslation")}
      isOpen
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid="add-translation-confirm-button"
          key="confirm"
          variant="primary"
          type="submit"
          form="translation-form"
        >
          {t("create")}
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
          {t("cancel")}
        </Button>,
      ]}
    >
      <Form id="translation-form" isHorizontal onSubmit={handleSubmit(save)}>
        <FormGroup
          label={t("key")}
          name="key"
          fieldId="key-id"
          helperTextInvalid={t("required")}
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
          label={t("value")}
          name="add-value"
          fieldId="value-id"
          helperTextInvalid={t("required")}
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
