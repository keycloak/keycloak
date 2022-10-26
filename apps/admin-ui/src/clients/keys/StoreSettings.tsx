import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup } from "@patternfly/react-core";

import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { PasswordInput } from "../../components/password-input/PasswordInput";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

export const StoreSettings = ({
  hidePassword = false,
  isSaml = false,
}: {
  hidePassword?: boolean;
  isSaml?: boolean;
}) => {
  const { t } = useTranslation("clients");
  const { register, errors } = useFormContext<KeyStoreConfig>();

  return (
    <>
      <FormGroup
        label={t("keyAlias")}
        fieldId="keyAlias"
        isRequired
        labelIcon={
          <HelpItem
            helpText="clients-help:keyAlias"
            fieldLabelId="clients:keyAlias"
          />
        }
        helperTextInvalid={t("common:required")}
        validated={errors.keyAlias ? "error" : "default"}
      >
        <KeycloakTextInput
          data-testid="keyAlias"
          type="text"
          id="keyAlias"
          name="keyAlias"
          ref={register({ required: true })}
          validated={errors.keyAlias ? "error" : "default"}
        />
      </FormGroup>
      {!hidePassword && (
        <FormGroup
          label={t("keyPassword")}
          fieldId="keyPassword"
          isRequired
          labelIcon={
            <HelpItem
              helpText="clients-help:keyPassword"
              fieldLabelId="clients:keyPassword"
            />
          }
          helperTextInvalid={t("common:required")}
          validated={errors.keyPassword ? "error" : "default"}
        >
          <PasswordInput
            data-testid="keyPassword"
            id="keyPassword"
            name="keyPassword"
            ref={register({ required: true })}
            validated={errors.keyPassword ? "error" : "default"}
          />
        </FormGroup>
      )}
      {isSaml && (
        <FormGroup
          label={t("realmCertificateAlias")}
          fieldId="realmCertificateAlias"
          labelIcon={
            <HelpItem
              helpText="clients-help:realmCertificateAlias"
              fieldLabelId="clients:realmCertificateAlias"
            />
          }
        >
          <KeycloakTextInput
            data-testid="realmCertificateAlias"
            type="text"
            id="realmCertificateAlias"
            name="realmAlias"
            ref={register()}
          />
        </FormGroup>
      )}
      <FormGroup
        label={t("storePassword")}
        fieldId="storePassword"
        isRequired
        labelIcon={
          <HelpItem
            helpText="clients-help:storePassword"
            fieldLabelId="clients:storePassword"
          />
        }
        helperTextInvalid={t("common:required")}
        validated={errors.storePassword ? "error" : "default"}
      >
        <PasswordInput
          data-testid="storePassword"
          id="storePassword"
          name="storePassword"
          ref={register({ required: true })}
          validated={errors.storePassword ? "error" : "default"}
        />
      </FormGroup>
    </>
  );
};
