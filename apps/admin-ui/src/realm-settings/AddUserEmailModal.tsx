import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextContent,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import { emailRegexPattern } from "../util";
import { useAdminClient } from "../context/auth/AdminClient";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import type { EmailRegistrationCallback } from "./EmailTab";

type AddUserEmailModalProps = {
  callback: EmailRegistrationCallback;
};

type AddUserEmailForm = {
  email: string;
};

export const AddUserEmailModal = ({ callback }: AddUserEmailModalProps) => {
  const { t } = useTranslation("groups");
  const { adminClient } = useAdminClient();
  const { whoAmI } = useWhoAmI();
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<AddUserEmailForm>({
    defaultValues: { email: "" },
  });

  const watchEmailInput = watch("email", "");
  const cancel = () => callback(false);
  const proceed = () => callback(true);

  const save = async (formData: AddUserEmailForm) => {
    await adminClient.users.update({ id: whoAmI.getUserId() }, formData);
    proceed();
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("realm-settings:provideEmailTitle")}
      isOpen={true}
      onClose={cancel}
      actions={[
        <Button
          data-testid="modal-test-connection-button"
          key="confirm"
          variant="primary"
          type="submit"
          form="email-form"
          isDisabled={!watchEmailInput}
        >
          {t("common:testConnection")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={cancel}
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
          <KeycloakTextInput
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
