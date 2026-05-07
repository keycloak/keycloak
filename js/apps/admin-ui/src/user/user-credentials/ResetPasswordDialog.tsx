import { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  AlertVariant,
  ButtonVariant,
  Form,
  FormGroup,
} from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormErrorText, PasswordInput } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { useAlerts } from "@keycloak/keycloak-ui-shared";
import {
  ConfirmDialogModal,
  useConfirmDialog,
} from "../../components/confirm-dialog/ConfirmDialog";
import useToggle from "../../utils/useToggle";

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
    register,
    formState: { isValid, errors },
    watch,
    handleSubmit,
    clearErrors,
    setError,
  } = form;

  const [confirm, toggle] = useToggle(true);
  const password = watch("password", "");
  const passwordConfirmation = watch("passwordConfirmation", "");

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
      const isLocalCredential =
        credentialLabel && credentialLabel.federationLink === undefined;

      if (isLocalCredential) {
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

  const { onChange, ...rest } = register("password", { required: true });
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
          <FormGroup
            name="password"
            label={t("password")}
            fieldId="password"
            isRequired
          >
            <PasswordInput
              data-testid="passwordField"
              id="password"
              onChange={async (e) => {
                await onChange(e);
                if (passwordConfirmation !== e.currentTarget.value) {
                  setError("passwordConfirmation", {
                    message: t("confirmPasswordDoesNotMatch"),
                  });
                } else {
                  clearErrors("passwordConfirmation");
                }
              }}
              {...rest}
            />
            {errors.password && <FormErrorText message={t("required")} />}
          </FormGroup>
          <FormGroup
            name="passwordConfirmation"
            label={
              isResetPassword
                ? t("resetPasswordConfirmation")
                : t("passwordConfirmation")
            }
            fieldId="passwordConfirmation"
            isRequired
          >
            <PasswordInput
              data-testid="passwordConfirmationField"
              id="passwordConfirmation"
              {...register("passwordConfirmation", {
                required: true,
                validate: (value) =>
                  value === password || t("confirmPasswordDoesNotMatch"),
              })}
            />
            {errors.passwordConfirmation && (
              <FormErrorText
                message={errors.passwordConfirmation.message as string}
              />
            )}
          </FormGroup>
          <FormProvider {...form}>
            <DefaultSwitchControl
              name="temporaryPassword"
              label={t("temporaryPassword")}
              labelIcon={t("temporaryPasswordHelpText")}
              className="pf-v5-u-mb-md"
              defaultValue="true"
            />
          </FormProvider>
        </Form>
      </ConfirmDialogModal>
    </>
  );
};
