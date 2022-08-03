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
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { capitalize } from "lodash-es";
import { useParams } from "react-router-dom";
import type FederatedIdentityRepresentation from "@keycloak/keycloak-admin-client/lib/defs/federatedIdentityRepresentation";
import type { UserParams } from "./routes/User";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";

type UserIdpModalProps = {
  federatedId?: string;
  handleModalToggle: () => void;
  refresh: (group?: GroupRepresentation) => void;
};

export const UserIdpModal = ({
  federatedId,
  handleModalToggle,
  refresh,
}: UserIdpModalProps) => {
  const { t } = useTranslation("users");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const {
    register,
    handleSubmit,
    formState: { isValid, errors },
  } = useForm({
    mode: "onChange",
  });

  const { id } = useParams<UserParams>();

  const submitForm = async (fedIdentity: FederatedIdentityRepresentation) => {
    try {
      await adminClient.users.addToFederatedIdentity({
        id: id!,
        federatedIdentityId: federatedId!,
        federatedIdentity: fedIdentity,
      });
      addAlert(t("users:idpLinkSuccess"), AlertVariant.success);
      handleModalToggle();
      refresh();
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
      isOpen={true}
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid={t("link")}
          key="confirm"
          variant="primary"
          type="submit"
          form="group-form"
          isDisabled={!isValid}
        >
          {t("link")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form id="group-form" onSubmit={handleSubmit(submitForm)}>
        <FormGroup
          name="idp-name-group"
          label={t("users:identityProvider")}
          fieldId="idp-name"
          helperTextInvalid={t("common:required")}
          validated={
            errors.identityProvider
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        >
          <KeycloakTextInput
            data-testid="idpNameInput"
            aria-label="Identity provider name input"
            ref={register({ required: true })}
            autoFocus
            isReadOnly
            type="text"
            id="link-idp-name"
            name="identityProvider"
            value={capitalize(federatedId)}
            validated={
              errors.identityProvider
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          />
        </FormGroup>
        <FormGroup
          name="user-id-group"
          label={t("users:userID")}
          fieldId="user-id"
          helperText={t("users-help:userIdHelperText")}
          helperTextInvalid={t("common:required")}
          validated={
            errors.userId ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <KeycloakTextInput
            data-testid="userIdInput"
            aria-label="user ID input"
            ref={register({ required: true })}
            autoFocus
            type="text"
            id="link-idp-user-id"
            name="userId"
            validated={
              errors.userId ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        </FormGroup>
        <FormGroup
          name="username-group"
          label={t("users:username")}
          fieldId="username"
          helperText={t("users-help:usernameHelperText")}
          helperTextInvalid={t("common:required")}
          validated={
            errors.name ? ValidatedOptions.error : ValidatedOptions.default
          }
          isRequired
        >
          <KeycloakTextInput
            data-testid="usernameInput"
            aria-label="username input"
            ref={register({ required: true })}
            autoFocus
            type="text"
            id="link-idp-username"
            name="userName"
            validated={
              errors.name ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
