import { FormErrorText, PasswordInput } from "@keycloak/keycloak-ui-shared";
import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { CredentialsForm } from "./ResetPasswordDialog";

type ResetPasswordFormProps = {
  isResetPassword?: boolean;
};

export const ResetPasswordForm = ({
  isResetPassword = true,
}: ResetPasswordFormProps) => {
  const { t } = useTranslation();
  const form = useFormContext<CredentialsForm>();
  const {
    register,
    formState: { errors },
    watch,
    clearErrors,
    setError,
  } = form;

  const password = watch("password", "");
  const passwordConfirmation = watch("passwordConfirmation", "");
  const { onChange, ...rest } = register("password", { required: true });

  return (
    <>
      <FormGroup
        name="password"
        label={t("password")}
        fieldId="password"
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
              value === password || t("confirmPasswordDoesNotMatch").toString(),
          })}
        />
        {errors.passwordConfirmation && (
          <FormErrorText
            message={errors.passwordConfirmation.message as string}
          />
        )}
      </FormGroup>
    </>
  );
};
