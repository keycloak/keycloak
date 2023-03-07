import { useTranslation } from "react-i18next";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { ModalVariant, Form, AlertVariant } from "@patternfly/react-core";

import type { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import { CredentialsResetActionMultiSelect } from "./CredentialsResetActionMultiSelect";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { LifespanField } from "./LifespanField";
import { isEmpty } from "lodash-es";

type ResetCredentialDialogProps = {
  userId: string;
  onClose: () => void;
};

type CredentialResetForm = {
  actions: RequiredActionAlias[];
  lifespan: number;
};

export const credResetFormDefaultValues: CredentialResetForm = {
  actions: [],
  lifespan: 43200, // 12 hours
};

export const ResetCredentialDialog = ({
  userId,
  onClose,
}: ResetCredentialDialogProps) => {
  const { t } = useTranslation("users");
  const form = useForm<CredentialResetForm>({
    defaultValues: credResetFormDefaultValues,
  });
  const { handleSubmit, control } = form;

  const resetActionWatcher = useWatch({
    control,
    name: "actions",
  });
  const resetIsNotDisabled = !isEmpty(resetActionWatcher);

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const sendCredentialsResetEmail = async ({
    actions,
    lifespan,
  }: CredentialResetForm) => {
    if (isEmpty(actions)) {
      return;
    }

    try {
      await adminClient.users.executeActionsEmail({
        id: userId,
        actions,
        lifespan,
      });
      addAlert(t("credentialResetEmailSuccess"), AlertVariant.success);
      onClose();
    } catch (error) {
      addError("users:credentialResetEmailError", error);
    }
  };

  return (
    <ConfirmDialogModal
      variant={ModalVariant.medium}
      titleKey="users:credentialReset"
      open
      onCancel={onClose}
      toggleDialog={onClose}
      continueButtonLabel="users:credentialResetConfirm"
      onConfirm={() => {
        handleSubmit(sendCredentialsResetEmail)();
      }}
      confirmButtonDisabled={!resetIsNotDisabled}
    >
      <Form
        id="userCredentialsReset-form"
        isHorizontal
        data-testid="credential-reset-modal"
      >
        <FormProvider {...form}>
          <CredentialsResetActionMultiSelect />
          <LifespanField />
        </FormProvider>
      </Form>
    </ConfirmDialogModal>
  );
};
