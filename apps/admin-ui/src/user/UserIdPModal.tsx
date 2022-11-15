import type FederatedIdentityRepresentation from "@keycloak/keycloak-admin-client/lib/defs/federatedIdentityRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  ValidatedOptions,
} from "@patternfly/react-core";
import { capitalize } from "lodash-es";
import { useForm } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { useAlerts } from "../components/alert/Alerts";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { useAdminClient } from "../context/auth/AdminClient";

type UserIdpModalProps = {
  userId: string;
  federatedId: string;
  onClose: () => void;
  onRefresh: () => void;
};

export const UserIdpModal = ({
  userId,
  federatedId,
  onClose,
  onRefresh,
}: UserIdpModalProps) => {
  const { t } = useTranslation("users");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const {
    register,
    handleSubmit,
    formState: { isValid, errors },
  } = useForm<FederatedIdentityRepresentation>({
    mode: "onChange",
  });

  const onSubmit = async (
    federatedIdentity: FederatedIdentityRepresentation
  ) => {
    try {
      await adminClient.users.addToFederatedIdentity({
        id: userId,
        federatedIdentityId: federatedId,
        federatedIdentity,
      });
      addAlert(t("users:idpLinkSuccess"), AlertVariant.success);
      onClose();
      onRefresh();
    } catch (error) {
      addError("users:couldNotLinkIdP", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("users:linkAccountTitle", {
        provider: capitalize(federatedId),
      })}
      onClose={onClose}
      actions={[
        <Button
          key="confirm"
          data-testid="confirm"
          variant="primary"
          type="submit"
          form="group-form"
          isDisabled={!isValid}
        >
          {t("link")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancel"
          variant={ButtonVariant.link}
          onClick={onClose}
        >
          {t("common:cancel")}
        </Button>,
      ]}
      isOpen
    >
      <Form id="group-form" onSubmit={handleSubmit(onSubmit)}>
        <FormGroup
          label={t("users:identityProvider")}
          fieldId="identityProvider"
        >
          <KeycloakTextInput
            id="identityProvider"
            data-testid="idpNameInput"
            value={capitalize(federatedId)}
            isReadOnly
          />
        </FormGroup>
        <FormGroup
          label={t("users:userID")}
          fieldId="userID"
          helperText={t("users-help:userIdHelperText")}
          helperTextInvalid={t("common:required")}
          validated={
            errors.userId ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <KeycloakTextInput
            id="userID"
            data-testid="userIdInput"
            validated={
              errors.userId ? ValidatedOptions.error : ValidatedOptions.default
            }
            autoFocus
            {...register("userId", { required: true })}
          />
        </FormGroup>
        <FormGroup
          label={t("users:username")}
          fieldId="username"
          helperText={t("users-help:usernameHelperText")}
          helperTextInvalid={t("common:required")}
          validated={
            errors.userName ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <KeycloakTextInput
            id="username"
            data-testid="usernameInput"
            validated={
              errors.userName
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            {...register("userName", { required: true })}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
