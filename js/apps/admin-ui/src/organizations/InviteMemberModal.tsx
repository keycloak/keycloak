import { FormSubmitButton, TextControl } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { ClientSelect } from "../components/client/ClientSelect";

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
      const { clientId, ...formFields } = data;
      const formData = new FormData();
      for (const key in formFields) {
        formData.append(key, formFields[key]);
      }
      await adminClient.organizations.invite({ orgId, clientId }, formData);
      addAlert(t("inviteSent"));
      onClose();
    } catch (error) {
      addError("inviteSentError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      isOpen
      onClose={onClose}
      aria-label={t("inviteNewUser")}
    >
      <ModalHeader title={t("inviteNewUser")} />
      <ModalBody>
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
            <ClientSelect
              name="clientId"
              label="client"
              helpText="invitationClientHelp"
            />
          </Form>
        </FormProvider>
      </ModalBody>
      <ModalFooter>
        <FormSubmitButton
          formState={formState}
          data-testid="save"
          key="confirm"
          form="form"
          allowInvalid
          allowNonDirty
        >
          {t("send")}
        </FormSubmitButton>
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("cancel")}
        </Button>
      </ModalFooter>
    </Modal>
  );
};
