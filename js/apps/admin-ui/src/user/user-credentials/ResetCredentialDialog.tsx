import type { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import { AlertVariant, Form, ModalVariant } from "@patternfly/react-core";
import { isEmpty } from "lodash-es";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { LifespanField } from "./LifespanField";
import { RequiredActionMultiSelect } from "./RequiredActionMultiSelect";

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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<CredentialResetForm>({
    defaultValues: credResetFormDefaultValues,
  });
  const { handleSubmit, control } = form;

  const resetActionWatcher = useWatch({
    control,
    name: "actions",
  });
  const resetIsNotDisabled = !isEmpty(resetActionWatcher);

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
      addError("credentialResetEmailError", error);
    }
  };

  return (
    <ConfirmDialogModal
      variant={ModalVariant.medium}
      titleKey="credentialReset"
      open
      onCancel={onClose}
      toggleDialog={onClose}
      continueButtonLabel="credentialResetConfirm"
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
          <RequiredActionMultiSelect
            name="actions"
            label="resetAction"
            help="resetActions"
          />
          <LifespanField />
        </FormProvider>
      </Form>
    </ConfirmDialogModal>
  );
};
