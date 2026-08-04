import { FormSubmitButton, TextControl } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { KeyValueInput } from "../components/key-value-form/KeyValueInput";
import {
  KeyValueType,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";

type InviteMemberForm = {
  email: string;
  firstName: string;
  lastName: string;
  attributes: KeyValueType[];
};

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
  const form = useForm<InviteMemberForm>();
  const { handleSubmit, formState } = form;

  const submitForm = async (data: InviteMemberForm) => {
    try {
      const attributes = keyValueToArray(data.attributes);
      await adminClient.organizations.invite(
        { orgId },
        {
          email: data.email,
          firstName: data.firstName,
          lastName: data.lastName,
          attributes,
        },
      );
      addAlert(t("inviteSent"));
      onClose();
    } catch (error) {
      addError("inviteSentError", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("inviteNewUser")}
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
          <FormGroup label={t("attributes")} fieldId="attributes">
            <KeyValueInput name="attributes" />
          </FormGroup>
        </Form>
      </FormProvider>
    </Modal>
  );
};
