import React from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
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

import { HelpItem } from "../../../components/help-enabler/HelpItem";

type AddSubFlowProps = {
  name: string;
  onConfirm: () => void;
  onCancel: () => void;
};

export const AddSubFlowModal = ({
  name,
  onConfirm,
  onCancel,
}: AddSubFlowProps) => {
  const { t } = useTranslation("authentication");
  const { register, errors, handleSubmit } = useForm();

  return (
    <Modal
      variant={ModalVariant.medium}
      isOpen={true}
      title={t("addStepTo", { name })}
      onClose={() => onCancel()}
      actions={[
        <Button
          id="modal-add"
          data-testid="modal-add"
          key="add"
          onClick={() => onConfirm()}
        >
          {t("common:add")}
        </Button>,
        <Button
          data-testid="cancel"
          id="modal-cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            onCancel();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form
        id="execution-config-form"
        isHorizontal
        onSubmit={handleSubmit(onConfirm)}
      >
        <FormGroup
          label={t("common:name")}
          fieldId="name"
          helperTextInvalid={t("common:required")}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
          labelIcon={
            <HelpItem
              helpText="authentication-help:name"
              forLabel={t("name")}
              forID="name"
            />
          }
        >
          <TextInput
            type="text"
            id="name"
            name="name"
            data-testid="name"
            ref={register({ required: true })}
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        </FormGroup>
        <FormGroup
          label={t("common:description")}
          fieldId="description"
          labelIcon={
            <HelpItem
              helpText="authentication-help:description"
              forLabel={t("common:description")}
              forID="description"
            />
          }
        >
          <TextInput
            type="text"
            id="description"
            name="description"
            data-testid="description"
            ref={register}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
