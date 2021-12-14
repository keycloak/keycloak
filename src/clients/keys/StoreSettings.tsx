import React from "react";
import { useTranslation } from "react-i18next";
import { FormGroup, TextInput } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { PasswordInput } from "../../components/password-input/PasswordInput";

export const StoreSettings = ({
  register,
  hidePassword = false,
}: {
  register: () => void;
  hidePassword?: boolean;
}) => {
  const { t } = useTranslation("clients");

  return (
    <>
      <FormGroup
        label={t("keyAlias")}
        fieldId="keyAlias"
        labelIcon={
          <HelpItem
            helpText="clients-help:keyAlias"
            fieldLabelId="clients:keyAlias"
          />
        }
      >
        <TextInput
          data-testid="keyAlias"
          type="text"
          id="keyAlias"
          name="keyAlias"
          ref={register}
        />
      </FormGroup>
      {!hidePassword && (
        <FormGroup
          label={t("keyPassword")}
          fieldId="keyPassword"
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
            ref={register}
          />
        </FormGroup>
      )}
      <FormGroup
        label={t("storePassword")}
        fieldId="storePassword"
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
          ref={register}
        />
      </FormGroup>
    </>
  );
};
