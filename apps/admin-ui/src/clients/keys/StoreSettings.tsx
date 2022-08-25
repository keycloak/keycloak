import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup } from "@patternfly/react-core";

import type KeyStoreConfig from "@keycloak/keycloak-admin-client/lib/defs/keystoreConfig";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { PasswordInput } from "../../components/password-input/PasswordInput";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";

export const StoreSettings = ({
  hidePassword = false,
}: {
  hidePassword?: boolean;
}) => {
  const { t } = useTranslation("clients");
  const { register } = useFormContext<KeyStoreConfig>();

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
      >
        <KeycloakTextInput
          data-testid="keyAlias"
          type="text"
          id="keyAlias"
          name="keyAlias"
          ref={register({ required: true })}
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
        >
          <PasswordInput
            data-testid="keyPassword"
            id="keyPassword"
            name="keyPassword"
            ref={register({ required: true })}
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
      >
        <PasswordInput
          data-testid="storePassword"
          id="storePassword"
          name="storePassword"
          ref={register({ required: true })}
        />
      </FormGroup>
    </>
  );
};
