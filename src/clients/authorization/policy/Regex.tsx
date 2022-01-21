import React from "react";
import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, TextInput } from "@patternfly/react-core";

import { HelpItem } from "../../../components/help-enabler/HelpItem";

export const Regex = () => {
  const { t } = useTranslation("clients");
  const { register, errors } = useFormContext();

  return (
    <>
      <FormGroup
        label={t("targetClaim")}
        fieldId="targetClaim"
        helperTextInvalid={t("common:required")}
        validated={errors.targetClaim ? "error" : "default"}
        isRequired
        labelIcon={
          <HelpItem
            helpText="clients-help:targetClaim"
            fieldLabelId="clients:targetClaim"
          />
        }
      >
        <TextInput
          type="text"
          id="targetClaim"
          name="targetClaim"
          data-testid="targetClaim"
          ref={register({ required: true })}
          validated={errors.targetClaim ? "error" : "default"}
        />
      </FormGroup>
      <FormGroup
        label={t("regexPattern")}
        fieldId="pattern"
        labelIcon={
          <HelpItem
            helpText="clients-help:regexPattern"
            fieldLabelId="clients:regexPattern"
          />
        }
        isRequired
        validated={errors.pattern ? "error" : "default"}
        helperTextInvalid={t("common:required")}
      >
        <TextInput
          ref={register({ required: true })}
          type="text"
          id="pattern"
          name="pattern"
          data-testid="regexPattern"
          validated={errors.pattern ? "error" : "default"}
        />
      </FormGroup>
    </>
  );
};
