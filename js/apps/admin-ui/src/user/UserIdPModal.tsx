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
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";

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
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const {
    register,
    handleSubmit,
    formState: { isValid, errors },
  } = useForm<FederatedIdentityRepresentation>({
    mode: "onChange",
  });

  const onSubmit = async (
    federatedIdentity: FederatedIdentityRepresentation,
  ) => {
    try {
      await adminClient.users.addToFederatedIdentity({
        id: userId,
        federatedIdentityId: federatedId,
        federatedIdentity,
      });
      addAlert(t("idpLinkSuccess"), AlertVariant.success);
      onClose();
      onRefresh();
    } catch (error) {
      addError("couldNotLinkIdP", error);
    }
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("linkAccountTitle", {
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
          {t("cancel")}
        </Button>,
      ]}
      isOpen
    >
      <Form id="group-form" onSubmit={handleSubmit(onSubmit)}>
        <FormGroup label={t("identityProvider")} fieldId="identityProvider">
          <KeycloakTextInput
            id="identityProvider"
            data-testid="idpNameInput"
            value={capitalize(federatedId)}
            isReadOnly
          />
        </FormGroup>
        <FormGroup
          label={t("userID")}
          fieldId="userID"
          helperText={t("userIdHelperText")}
          helperTextInvalid={t("required")}
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
          label={t("username")}
          fieldId="username"
          helperText={t("usernameHelperText")}
          helperTextInvalid={t("required")}
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
