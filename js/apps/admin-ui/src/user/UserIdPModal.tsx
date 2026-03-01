import type FederatedIdentityRepresentation from "@keycloak/keycloak-admin-client/lib/defs/federatedIdentityRepresentation";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  TextInput,
} from "@patternfly/react-core";
import { capitalize } from "lodash-es";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";

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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const form = useForm<FederatedIdentityRepresentation>({
    mode: "onChange",
  });
  const {
    handleSubmit,
    formState: { isValid },
  } = form;

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
        <FormProvider {...form}>
          <FormGroup label={t("identityProvider")} fieldId="identityProvider">
            <TextInput
              id="identityProvider"
              data-testid="idpNameInput"
              value={capitalize(federatedId)}
              readOnly
            />
          </FormGroup>
          <TextControl
            name="userId"
            label={t("userID")}
            helperText={t("userIdHelperText")}
            autoFocus
            rules={{
              required: t("required"),
            }}
          />
          <TextControl
            name="userName"
            label={t("username")}
            helperText={t("usernameHelperText")}
            rules={{
              required: t("required"),
            }}
          />
        </FormProvider>
      </Form>
    </Modal>
  );
};
