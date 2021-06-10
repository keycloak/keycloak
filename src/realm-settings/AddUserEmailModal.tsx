import React from "react";
import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextContent,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm, UseFormMethods } from "react-hook-form";

import type UserRepresentation from "keycloak-admin/lib/defs/userRepresentation";
import { emailRegexPattern } from "../util";

type AddUserEmailModalProps = {
  id?: string;
  form: UseFormMethods<UserRepresentation>;
  rename?: string;
  handleModalToggle: () => void;
  testConnection: () => void;
  user: UserRepresentation;
  save: (user?: UserRepresentation) => void;
};

export const AddUserEmailModal = ({
  handleModalToggle,
  save,
}: AddUserEmailModalProps) => {
  const { t } = useTranslation("groups");
  const { register, errors, handleSubmit, watch } = useForm();

  const watchEmailInput = watch("email", "");

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("realm-settings:provideEmailTitle")}
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid="modal-test-connection-button"
          key="confirm"
          variant="primary"
          type="submit"
          form="email-form"
          isDisabled={!watchEmailInput}
        >
          {t("realm-settings:testConnection")}
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
      <TextContent className="kc-provide-email-text">
        {t("realm-settings:provideEmail")}
      </TextContent>
      <Form id="email-form" isHorizontal onSubmit={handleSubmit(save)}>
        <FormGroup
          className="kc-email-form-group"
          name="add-email-address"
          fieldId="email-id"
          helperTextInvalid={t("users:emailInvalid")}
          validated={
            errors.email ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <TextInput
            data-testid="email-address-input"
            ref={register({ required: true, pattern: emailRegexPattern })}
            autoFocus
            type="text"
            id="add-email"
            name="email"
            validated={
              errors.email ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
