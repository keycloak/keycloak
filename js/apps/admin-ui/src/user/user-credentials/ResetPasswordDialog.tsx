import { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { AlertVariant, ButtonVariant, Form } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import {
  ConfirmDialogModal,
  useConfirmDialog,
} from "../../components/confirm-dialog/ConfirmDialog";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import useToggle from "../../utils/useToggle";
import { ResetPasswordForm } from "./ResetPasswordForm";

type ResetPasswordDialogProps = {
  user: UserRepresentation;
  isResetPassword: boolean;
  onAddRequiredActions?: (requiredActions: string[]) => void;
  refresh: () => void;
  onClose: () => void;
};

export type CredentialsForm = {
  password: string;
  passwordConfirmation: string;
  temporaryPassword: boolean;
};

const credFormDefaultValues: CredentialsForm = {
  password: "",
  passwordConfirmation: "",
  temporaryPassword: true,
};

export const ResetPasswordDialog = ({
  user,
  isResetPassword,
  onAddRequiredActions,
  refresh,
  onClose,
}: ResetPasswordDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<CredentialsForm>({
    defaultValues: credFormDefaultValues,
    mode: "onChange",
  });
  const {
    formState: { isValid },
    handleSubmit,
  } = form;

  const [confirm, toggle] = useToggle(true);

  const { addAlert, addError } = useAlerts();

  const [toggleConfirmSaveModal, ConfirmSaveModal] = useConfirmDialog({
    titleKey: isResetPassword ? "resetPasswordConfirm" : "setPasswordConfirm",
    messageKey: isResetPassword
      ? t("resetPasswordConfirmText", { username: user.username })
      : t("setPasswordConfirmText", { username: user.username }),
    continueButtonLabel: isResetPassword ? "resetPassword" : "savePassword",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: () => handleSubmit(saveUserPassword)(),
  });

  const saveUserPassword = async ({
    password,
    temporaryPassword,
  }: CredentialsForm) => {
    try {
      await adminClient.users.resetPassword({
        id: user.id!,
        credential: {
          temporary: temporaryPassword,
          type: "password",
          value: password,
        },
      });
      if (temporaryPassword) {
        onAddRequiredActions?.([RequiredActionAlias.UPDATE_PASSWORD]);
      }
      const credentials = await adminClient.users.getCredentials({
        id: user.id!,
      });
      const credentialLabel = credentials.find((c) => c.type === "password");
      if (credentialLabel) {
        await adminClient.users.updateCredentialLabel(
          {
            id: user.id!,
            credentialId: credentialLabel.id!,
          },
          t("defaultPasswordLabel"),
        );
      }
      addAlert(
        isResetPassword
          ? t("resetCredentialsSuccess")
          : t("savePasswordSuccess"),
        AlertVariant.success,
      );
      refresh();
    } catch (error) {
      addError(
        isResetPassword ? "resetPasswordError" : "savePasswordError",
        error,
      );
    }

    onClose();
  };

  return (
    <>
      <ConfirmSaveModal />
      <ConfirmDialogModal
        titleKey={
          isResetPassword
            ? t("resetPasswordFor", { username: user.username })
            : t("setPasswordFor", { username: user.username })
        }
        open={confirm}
        onCancel={onClose}
        toggleDialog={toggle}
        onConfirm={toggleConfirmSaveModal}
        confirmButtonDisabled={!isValid}
        continueButtonLabel="save"
      >
        <Form
          id="userCredentials-form"
          isHorizontal
          className="keycloak__user-credentials__reset-form"
        >
          <FormProvider {...form}>
            <ResetPasswordForm isResetPassword={isResetPassword} />
            <DefaultSwitchControl
              name="temporaryPassword"
              label={t("temporaryPassword")}
              labelIcon={t("temporaryPasswordHelpText")}
              defaultValue="true"
            />
          </FormProvider>
        </Form>
      </ConfirmDialogModal>
    </>
  );
};
