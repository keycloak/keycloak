import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import {
  SelectControl,
  TextControl,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../../admin-client";

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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<Flow>();
  const [formProviders, setFormProviders] =
    useState<AuthenticationProviderRepresentation[]>();

  useFetch(
    () => adminClient.authenticationManagement.getFormProviders(),
    setFormProviders,
    [],
  );

  useEffect(() => {
    if (formProviders?.length === 1) {
      form.setValue("provider", formProviders[0].id!);
    }
  }, [formProviders]);

  return (
    <Modal
      variant={ModalVariant.medium}
      title={t("addSubFlowTo", { name })}
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
      <Form
        id="sub-flow-form"
        onSubmit={form.handleSubmit(onConfirm)}
        isHorizontal
      >
        <FormProvider {...form}>
          <TextControl
            name="name"
            label={t("name")}
            labelIcon={t("clientIdHelp")}
            rules={{ required: t("required") }}
          />
          <TextControl
            name="description"
            label={t("description")}
            labelIcon={t("flowNameDescriptionHelp")}
          />
          <SelectControl
            name="type"
            menuAppendTo="parent"
            label={t("flowType")}
            options={types.map((type) => ({
              key: type,
              value: t(`flow-type.${type}`),
            }))}
            controller={{ defaultValue: types[0] }}
          />
          {formProviders && formProviders.length > 1 && (
            <SelectControl
              name="provider"
              label={t("provider")}
              labelIcon={t("authenticationFlowTypeHelp")}
              options={formProviders.map((provider) => ({
                key: provider.id!,
                value: provider.displayName!,
              }))}
              controller={{ defaultValue: "" }}
            />
          )}
        </FormProvider>
      </Form>
    </Modal>
  );
};
