import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup } from "@patternfly/react-core";

import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { HelpItem } from "ui-shared";
import { PasswordInput } from "../../components/password-input/PasswordInput";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

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
      <FormGroup
        label={t("keyAlias")}
        fieldId="keyAlias"
        isRequired
        labelIcon={
          <HelpItem helpText={t("keyAliasHelp")} fieldLabelId="keyAlias" />
        }
        helperTextInvalid={t("required")}
        validated={errors.keyAlias ? "error" : "default"}
      >
        <KeycloakTextInput
          data-testid="keyAlias"
          id="keyAlias"
          validated={errors.keyAlias ? "error" : "default"}
          {...register("keyAlias", { required: true })}
        />
      </FormGroup>
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
        <FormGroup
          label={t("realmCertificateAlias")}
          fieldId="realmCertificateAlias"
          labelIcon={
            <HelpItem
              helpText={t("realmCertificateAliasHelp")}
              fieldLabelId="realmCertificateAlias"
            />
          }
        >
          <KeycloakTextInput
            data-testid="realmCertificateAlias"
            id="realmCertificateAlias"
            {...register("realmAlias")}
          />
        </FormGroup>
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
