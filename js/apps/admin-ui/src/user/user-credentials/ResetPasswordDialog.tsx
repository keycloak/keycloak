import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import {
  AlertVariant,
  ButtonVariant,
  Form,
  FormGroup,
  Switch,
  ValidatedOptions,
} from "@patternfly/react-core";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import {
  ConfirmDialogModal,
  useConfirmDialog,
} from "../../components/confirm-dialog/ConfirmDialog";
import { PasswordInput } from "../../components/password-input/PasswordInput";
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
  const { t } = useTranslation();
  const {
    register,
    control,
    formState: { isValid, errors },
    watch,
    handleSubmit,
    clearErrors,
    setError,
  } = useForm<CredentialsForm>({
    defaultValues: credFormDefaultValues,
    mode: "onChange",
  });

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
            helperTextInvalid={t("required")}
            validated={
              errors.password
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            isRequired
          >
            <PasswordInput
              data-testid="passwordField"
              id="password"
              onChange={(e) => {
                onChange(e);
                if (passwordConfirmation !== e.currentTarget.value) {
                  setError("passwordConfirmation", {
                    message: t("confirmPasswordDoesNotMatch").toString(),
                  });
                } else {
                  clearErrors("passwordConfirmation");
                }
              }}
              {...rest}
            />
          </FormGroup>
          <FormGroup
            name="passwordConfirmation"
            label={
              isResetPassword
                ? t("resetPasswordConfirmation")
                : t("passwordConfirmation")
            }
            fieldId="passwordConfirmation"
            helperTextInvalid={errors.passwordConfirmation?.message}
            validated={
              errors.passwordConfirmation
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            isRequired
          >
            <PasswordInput
              data-testid="passwordConfirmationField"
              id="passwordConfirmation"
              {...register("passwordConfirmation", {
                required: true,
                validate: (value) =>
                  value === password ||
                  t("confirmPasswordDoesNotMatch").toString(),
              })}
            />
          </FormGroup>
          <FormGroup
            label={t("temporaryPassword")}
            labelIcon={
              <HelpItem
                helpText={t("temporaryPasswordHelpText")}
                fieldLabelId="temporaryPassword"
              />
            }
            fieldId="kc-temporaryPassword"
          >
            <Controller
              name="temporaryPassword"
              defaultValue={true}
              control={control}
              render={({ field }) => (
                <Switch
                  className="kc-temporaryPassword"
                  onChange={field.onChange}
                  isChecked={field.value}
                  label={t("on")}
                  labelOff={t("off")}
                  aria-label={t("temporaryPassword")}
                />
              )}
            />
          </FormGroup>
        </Form>
      </ConfirmDialogModal>
    </>
  );
};
