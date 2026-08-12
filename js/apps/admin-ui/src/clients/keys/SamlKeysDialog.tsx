import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
  Text,
  TextContent,
  Title,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { KeyForm } from "./GenerateKeyDialog";
import type { KeyTypes } from "./SamlKeys";

type SamlKeysDialogProps = {
  id: string;
  attr: KeyTypes;
  localeKey: string;
  onClose: () => void;
  onCancel: () => void;
};

export type SamlKeysDialogForm = {
  file: File;
  format: string;
  keyAlias: string;
  storePassword: string;
  keyPassword: string;
};

export const submitForm = async (
  adminClient: KeycloakAdminClient,
  form: SamlKeysDialogForm,
  id: string,
  attr: KeyTypes,
  callback: (error?: unknown) => void,
) => {
  try {
    const formData = new FormData();
    const { file, ...rest } = form;
    Object.entries(rest).map(([key, value]) =>
      formData.append(key === "format" ? "keystoreFormat" : key, value),
    );
    formData.append("file", file);

    await adminClient.clients.uploadKey({ id, attr }, formData);
    callback();
  } catch (error) {
    callback(error);
  }
};

export const SamlKeysDialog = ({
  id,
  attr,
  localeKey,
  onClose,
  onCancel,
}: SamlKeysDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<SamlKeysDialogForm>({ mode: "onChange" });
  const {
    handleSubmit,
    formState: { isValid },
  } = form;

  const { addAlert, addError } = useAlerts();

  const submit = async (form: SamlKeysDialogForm) => {
    await submitForm(adminClient, form, id, attr, (error) => {
      if (error) {
        addError("importError", error);
      } else {
        addAlert(t("importSuccess"), AlertVariant.success);
      }
    });
  };

  return (
    <Modal
      variant={ModalVariant.medium}
      aria-label={t("enableClientSignatureRequiredModal")}
      header={
        <TextContent>
          <Title headingLevel="h1">
            {t("enableClientSignatureRequired", {
              key: t(localeKey),
            })}
          </Title>
          <Text>
            {t("enableClientSignatureRequiredExplain", {
              key: t(localeKey),
            })}
          </Text>
        </TextContent>
      }
      isOpen={true}
      onClose={onClose}
      actions={[
        <Button
          id="modal-confirm"
          key="confirm"
          data-testid="confirm"
          variant="primary"
          isDisabled={!isValid}
          onClick={async () => {
            await handleSubmit(submit)();
            onClose();
          }}
        >
          {t("confirm")}
        </Button>,
        <Button
          id="modal-cancel"
          key="cancel"
          data-testid="cancel"
          variant={ButtonVariant.link}
          onClick={onCancel}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form isHorizontal>
          <KeyForm useFile hasPem />
        </Form>
      </FormProvider>
    </Modal>
  );
};
