import type UserVerifiableCredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userVerifiableCredentialRepresentation";
import { AlertVariant, Form, ModalVariant } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { TimeSelectorControl } from "../../components/time-selector/TimeSelectorControl";
import { useRealm } from "../../context/realm-context/RealmContext";

type UserVerifiableCredentialOfferDialogProps = {
  userId: string;
  credential: UserVerifiableCredentialRepresentation;
  onClose: () => void;
};

type UserVerifiableCredentialOfferForm = {
  lifespan: number | undefined;
};

export const UserVerifiableCredentialOfferDialog = ({
  userId,
  credential,
  onClose,
}: UserVerifiableCredentialOfferDialogProps) => {
  const { adminClient } = useAdminClient();
  const { realmRepresentation: realm } = useRealm();
  const { t } = useTranslation();
  const form = useForm<UserVerifiableCredentialOfferForm>({
    defaultValues: {
      lifespan: realm.actionTokenGeneratedByAdminLifespan,
    },
  });
  const { handleSubmit } = form;

  const { addAlert, addError } = useAlerts();

  const sendCredentialOfferEmail = async ({
    lifespan,
  }: UserVerifiableCredentialOfferForm) => {
    try {
      await adminClient.users.sendVerifiableCredentialOffer(
        {
          id: userId,
          lifespan,
        },
        { credentialConfigurationId: credential.credentialConfigurationId },
      );
      addAlert(t("credentialOfferEmailSuccess"), AlertVariant.success);
      onClose();
    } catch (error) {
      addError("credentialOfferEmailError", error);
    }
  };

  return (
    <ConfirmDialogModal
      variant={ModalVariant.medium}
      titleKey="credentialOfferSend"
      open
      onCancel={onClose}
      toggleDialog={onClose}
      continueButtonLabel="credentialOfferSendConfirm"
      onConfirm={async () => {
        await handleSubmit(sendCredentialOfferEmail)();
      }}
    >
      <Form
        id="userVerifiableCredentialOffer-form"
        isHorizontal
        data-testid="credential-offer-modal"
      >
        <FormProvider {...form}>
          <TimeSelectorControl
            name="lifespan"
            label={t("lifespan")}
            labelIcon={t("sendCredentialOfferLifespanHelp")}
            units={["minute", "hour", "day"]}
            menuAppendTo="parent"
            controller={{}}
          />
        </FormProvider>
      </Form>
    </ConfirmDialogModal>
  );
};
