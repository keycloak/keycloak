import React from "react";
import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, TextInput, ValidatedOptions } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { PasswordInput } from "../../components/password-input/PasswordInput";

export const ClientIdSecret = ({
  secretRequired = true,
  create = true,
}: {
  secretRequired?: boolean;
  create?: boolean;
}) => {
  const { t } = useTranslation("identity-providers");
  const { t: th } = useTranslation("identity-providers-help");

  const { register, errors } = useFormContext();

  return (
    <>
      <FormGroup
        label={t("clientId")}
        labelIcon={
          <HelpItem
            helpText={th("clientId")}
            forLabel={t("clientId")}
            forID="kc-client-id"
          />
        }
        fieldId="kc-client-id"
        isRequired
        validated={
          errors.config?.clientId
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          isRequired
          type="text"
          id="kc-client-id"
          data-testid="clientId"
          name="config.clientId"
          ref={register({ required: true })}
        />
      </FormGroup>
      <FormGroup
        label={t("clientSecret")}
        labelIcon={
          <HelpItem
            helpText={th("clientSecret")}
            forLabel={t("clientSecret")}
            forID="kc-client-secret"
          />
        }
        fieldId="kc-client-secret"
        isRequired={secretRequired}
        validated={
          errors.config?.clientSecret
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
        helperTextInvalid={t("common:required")}
      >
        {create && (
          <PasswordInput
            isRequired={secretRequired}
            id="kc-client-secret"
            data-testid="clientSecret"
            name="config.clientSecret"
            ref={register({ required: secretRequired })}
          />
        )}
        {!create && (
          <TextInput
            isRequired={secretRequired}
            type="password"
            id="kc-client-secret"
            data-testid="clientSecret"
            name="config.clientSecret"
            ref={register({ required: secretRequired })}
          />
        )}
      </FormGroup>
    </>
  );
};
