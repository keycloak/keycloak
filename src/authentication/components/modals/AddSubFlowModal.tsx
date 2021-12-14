import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm } from "react-hook-form";
import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";

import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { useAdminClient, useFetch } from "../../../context/auth/AdminClient";

type AddSubFlowProps = {
  name: string;
  onConfirm: (flow: Flow) => void;
  onCancel: () => void;
};

const types = ["basic-flow", "form-flow"] as const;

export type Flow = {
  name: string;
  description?: string;
  type: typeof types[number];
  provider: string;
};

export const AddSubFlowModal = ({
  name,
  onConfirm,
  onCancel,
}: AddSubFlowProps) => {
  const { t } = useTranslation("authentication");
  const { register, control, errors, handleSubmit } = useForm<Flow>();
  const [open, setOpen] = useState(false);
  const [openProvider, setOpenProvider] = useState(false);
  const [formProviders, setFormProviders] =
    useState<AuthenticationProviderRepresentation[]>();
  const adminClient = useAdminClient();

  useFetch(
    () => adminClient.authenticationManagement.getFormProviders(),
    (providers) =>
      setFormProviders(
        providers as unknown as AuthenticationProviderRepresentation[]
      ),
    []
  );

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
          type="submit"
          form="sub-flow-form"
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
      <Form id="sub-flow-form" isHorizontal onSubmit={handleSubmit(onConfirm)}>
        <FormGroup
          label={t("common:name")}
          fieldId="name"
          helperTextInvalid={t("common:required")}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
          labelIcon={
            <HelpItem helpText="authentication-help:name" fieldLabelId="name" />
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
              fieldLabelId="description"
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
        <FormGroup
          label={t("flowType")}
          labelIcon={
            <HelpItem
              helpText="authentication-help:flowType"
              fieldLabelId="authentication:flowType"
            />
          }
          fieldId="flowType"
        >
          <Controller
            name="type"
            defaultValue={types[0]}
            control={control}
            render={({ onChange, value }) => (
              <Select
                menuAppendTo="parent"
                toggleId="flowType"
                onToggle={setOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setOpen(false);
                }}
                selections={t(`flow-type.${value}`)}
                variant={SelectVariant.single}
                aria-label={t("flowType")}
                isOpen={open}
              >
                {types.map((type) => (
                  <SelectOption
                    selected={type === value}
                    key={type}
                    value={type}
                  >
                    {t(`flow-type.${type}`)}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        </FormGroup>
        {formProviders && formProviders.length > 1 && (
          <FormGroup
            label={t("flowType")}
            labelIcon={
              <HelpItem
                helpText="authentication-help:flowType"
                fieldLabelId="authentication:flowType"
              />
            }
            fieldId="flowType"
          >
            <Controller
              name="provider"
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <Select
                  menuAppendTo="parent"
                  toggleId="provider"
                  onToggle={setOpenProvider}
                  onSelect={(_, value) => {
                    onChange(value.toString());
                    setOpenProvider(false);
                  }}
                  selections={value.displayName}
                  variant={SelectVariant.single}
                  aria-label={t("flowType")}
                  isOpen={openProvider}
                >
                  {formProviders.map((provider) => (
                    <SelectOption
                      selected={provider.displayName === value}
                      key={provider.id}
                      value={provider}
                    >
                      {provider.displayName}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
        )}
        {formProviders?.length === 1 && (
          <input
            name="provider"
            type="hidden"
            ref={register}
            value={formProviders[0].id}
          />
        )}
      </Form>
    </Modal>
  );
};
