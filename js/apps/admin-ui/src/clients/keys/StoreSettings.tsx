import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { FormGroup } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "ui-shared";
import { PasswordInput } from "../../components/password-input/PasswordInput";

export const StoreSettings = ({
  hidePassword = false,
  isSaml = false,
}: {
  hidePassword?: boolean;
  isSaml?: boolean;
}) => {
  const { t } = useTranslation();
  const {
    register,
    formState: { errors },
  } = useFormContext<KeyStoreConfig>();

  return (
    <>
      <TextControl
        name="keyAlias"
        label={t("keyAlias")}
        labelIcon={t("keyAliasHelp")}
        rules={{
          required: t("required"),
        }}
      />
      {!hidePassword && (
        <FormGroup
          label={t("keyPassword")}
          fieldId="keyPassword"
          isRequired
          labelIcon={
            <HelpItem
              helpText={t("keyPasswordHelp")}
              fieldLabelId="keyPassword"
            />
          }
          helperTextInvalid={t("required")}
          validated={errors.keyPassword ? "error" : "default"}
        >
          <PasswordInput
            data-testid="keyPassword"
            id="keyPassword"
            validated={errors.keyPassword ? "error" : "default"}
            {...register("keyPassword", { required: true })}
          />
        </FormGroup>
      )}
      {isSaml && (
        <TextControl
          name="realmAlias"
          label={t("realmCertificateAlias")}
          labelIcon={t("realmCertificateAliasHelp")}
        />
      )}
      <FormGroup
        label={t("storePassword")}
        fieldId="storePassword"
        isRequired
        labelIcon={
          <HelpItem
            helpText={t("storePasswordHelp")}
            fieldLabelId="storePassword"
          />
        }
        helperTextInvalid={t("required")}
        validated={errors.storePassword ? "error" : "default"}
      >
        <PasswordInput
          data-testid="storePassword"
          id="storePassword"
          validated={errors.storePassword ? "error" : "default"}
          {...register("storePassword", { required: true })}
        />
      </FormGroup>
    </>
  );
};
