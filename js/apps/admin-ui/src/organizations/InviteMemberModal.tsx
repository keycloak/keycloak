import { FormSubmitButton, TextControl } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

type InviteMemberModalProps = {
  orgId: string;
  onClose: () => void;
};

export const InviteMemberModal = ({
  orgId,
  onClose,
}: InviteMemberModalProps) => {
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const { t } = useTranslation();
  const form = useForm<Record<string, string>>();
  const { handleSubmit, formState } = form;

  const submitForm = async (data: Record<string, string>) => {
    try {
      const formData = new FormData();
      for (const key in data) {
        formData.append(key, data[key]);
      }
      await adminClient.organizations.invite({ orgId }, formData);
      addAlert(t("inviteSent"));
      onClose();
    } catch (error) {
      addError("inviteSentError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("inviteMember")}
      isOpen
      onClose={onClose}
      actions={[
        <FormSubmitButton
          formState={formState}
          data-testid="save"
          key="confirm"
          form="form"
          allowInvalid
          allowNonDirty
        >
          {t("send")}
        </FormSubmitButton>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <Form id="form" onSubmit={handleSubmit(submitForm)}>
          <TextControl
            name="email"
            label={t("email")}
            rules={{ required: t("required") }}
            autoFocus
          />
          <TextControl name="firstName" label={t("firstName")} />
          <TextControl name="lastName" label={t("lastName")} />
        </Form>
      </FormProvider>
    </Modal>
  );
};
