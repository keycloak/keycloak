import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
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
  ValidatedOptions,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../../admin-client";
import { KeycloakTextInput } from "../../../components/keycloak-text-input/KeycloakTextInput";
import { useFetch } from "../../../utils/useFetch";

type AddSubFlowProps = {
  name: string;
  onConfirm: (flow: Flow) => void;
  onCancel: () => void;
};

const types = ["basic-flow", "form-flow"] as const;

export type Flow = {
  name: string;
  description: string;
  type: (typeof types)[number];
  provider: string;
};

export const AddSubFlowModal = ({
  name,
  onConfirm,
  onCancel,
}: AddSubFlowProps) => {
  const { t } = useTranslation();
  const {
    register,
    control,
    setValue,
    handleSubmit,
    formState: { errors },
  } = useForm<Flow>();
  const [open, setOpen] = useState(false);
  const [openProvider, setOpenProvider] = useState(false);
  const [formProviders, setFormProviders] =
    useState<AuthenticationProviderRepresentation[]>();

  useFetch(
    () => adminClient.authenticationManagement.getFormProviders(),
    setFormProviders,
    [],
  );

  useEffect(() => {
    if (formProviders?.length === 1) {
      setValue("provider", formProviders[0].id!);
    }
  }, [formProviders]);

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("addStepTo", { name })}
      onClose={onCancel}
      actions={[
        <Button
          key="add"
          data-testid="modal-add"
          type="submit"
          form="sub-flow-form"
        >
          {t("add")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancel"
          variant={ButtonVariant.link}
          onClick={onCancel}
        >
          {t("cancel")}
        </Button>,
      ]}
      isOpen
    >
      <Form id="sub-flow-form" onSubmit={handleSubmit(onConfirm)} isHorizontal>
        <FormGroup
          label={t("name")}
          fieldId="name"
          helperTextInvalid={t("required")}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          labelIcon={
            <HelpItem helpText={t("flowNameHelp")} fieldLabelId="name" />
          }
          isRequired
        >
          <KeycloakTextInput
            id="name"
            data-testid="name"
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
            {...register("name", { required: true })}
          />
        </FormGroup>
        <FormGroup
          label={t("description")}
          fieldId="description"
          labelIcon={
            <HelpItem
              helpText={t("flowNameDescriptionHelp")}
              fieldLabelId="description"
            />
          }
        >
          <KeycloakTextInput
            id="description"
            data-testid="description"
            {...register("description")}
          />
        </FormGroup>
        <FormGroup
          label={t("flowType")}
          fieldId="flowType"
          labelIcon={
            <HelpItem helpText={t("flowTypeHelp")} fieldLabelId="flowType" />
          }
        >
          <Controller
            name="type"
            defaultValue={types[0]}
            control={control}
            render={({ field }) => (
              <Select
                menuAppendTo="parent"
                toggleId="flowType"
                onToggle={setOpen}
                onSelect={(_, value) => {
                  field.onChange(value.toString());
                  setOpen(false);
                }}
                selections={t(`flow-type.${field.value}`)}
                variant={SelectVariant.single}
                isOpen={open}
              >
                {types.map((type) => (
                  <SelectOption
                    key={type}
                    value={type}
                    selected={type === field.value}
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
                helpText={t("authenticationFlowTypeHelp")}
                fieldLabelId="flowType"
              />
            }
            fieldId="flowType"
          >
            <Controller
              name="provider"
              defaultValue=""
              control={control}
              render={({ field }) => (
                <Select
                  menuAppendTo="parent"
                  toggleId="provider"
                  onToggle={setOpenProvider}
                  onSelect={(_, value) => {
                    field.onChange(value.toString());
                    setOpenProvider(false);
                  }}
                  selections={field.value}
                  variant={SelectVariant.single}
                  isOpen={openProvider}
                >
                  {formProviders.map((provider) => (
                    <SelectOption
                      key={provider.id}
                      value={provider.id}
                      selected={provider.displayName === field.value}
                    >
                      {provider.displayName}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
        )}
      </Form>
    </Modal>
  );
};
