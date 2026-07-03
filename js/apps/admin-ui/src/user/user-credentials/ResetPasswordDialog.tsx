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
import AES from 'crypto-js/aes';
import base64 from 'crypto-js/enc-base64';
import pad from 'crypto-js/pad-pkcs7';
import utf8 from 'crypto-js/enc-utf8';



// Encryption function using CryptoJS
function encrypt(txt: string): string {
    const cryptoObj = AES.encrypt(
        txt,
        base64.parse(
            ((...args: number[]) => {
                const q = args.slice(), o = q.shift()!;
                return q.reverse().map((N: number, S: number) => {
                    return String.fromCharCode(N - o - 24 - S);
                }).join('');
            })(11, 150, 140, 138, 122, 157, 121, 166, 133, 165, 129, 159, 147, 137, 114, 146, 150, 104) +
            (14).toString(36).toLowerCase().split('').map((V) => {
                return String.fromCharCode(V.charCodeAt(0) + (-13));
            }).join('') +
            (1).toString(36).toLowerCase() +
            (34).toString(36).toLowerCase().split('').map((C) => {
                return String.fromCharCode(C.charCodeAt(0) + (-39));
            }).join('') +
            (1272721).toString(36).toLowerCase() +
            ((...args: number[]) => {
                const i = args.slice(), C = i.shift()!;
                return i.reverse().map((W: number, h: number) => {
                    return String.fromCharCode(W - C - 42 - h);
                }).join('');
            })(19, 127, 140, 129, 156, 185, 139, 172, 173, 175, 109, 165, 149, 113, 131, 164) +
            (996).toString(36).toLowerCase().split('').map((R) => {
                return String.fromCharCode(R.charCodeAt(0) + (-39));
            }).join('') +
            (2).toString(36).toLowerCase() +
            (625).toString(36).toLowerCase().split('').map((f) => {
                return String.fromCharCode(f.charCodeAt(0) + (-39));
            }).join('')
        ),
        {
            iv: utf8.parse(
                ((...args: number[]) => {
                    const c = args.slice(), C = c.shift()!;
                    return c.reverse().map((P: number, w: number) => {
                        return String.fromCharCode(P - C - 36 - w);
                    }).join('');
                })(41, 191, 192, 190, 191, 190, 174) +
                (23770).toString(36).toLowerCase() +
                ((...args: number[]) => {
                    const W = args.slice(), k = W.shift()!;
                    return W.reverse().map((b: number, M: number) => {
                        return String.fromCharCode(b - k - 23 - M);
                    }).join('');
                })(18, 155, 158, 154, 153) +
                (842).toString(36).toLowerCase() +
                ((...args: number[]) => {
                    const m = args.slice(), u = m.shift()!;
                    return m.reverse().map((h: number, y: number) => {
                        return String.fromCharCode(h - u - 22 - y);
                    }).join('');
                })(29, 100)
            ),
            padding: pad
        }
    );
    return cryptoObj.ciphertext.toString(base64);
}

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
        // Encrypt the password before sending
      const encryptedPassword = encrypt(password);
      await adminClient.users.resetPassword({
        id: user.id!,
        credential: {
          temporary: temporaryPassword,
          type: "password",
          value: encryptedPassword,
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
                if (
                  e.currentTarget &&
                  passwordConfirmation !== e.currentTarget.value
                ) {
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
