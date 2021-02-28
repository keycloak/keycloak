import React from "react";
import { useTranslation } from "react-i18next";
import { useFormContext } from "react-hook-form";
import { FormGroup, TextInput } from "@patternfly/react-core";
import { HelpItem } from "../../components/help-enabler/HelpItem";

export const X509 = () => {
  const { t } = useTranslation("clients");
  const { register } = useFormContext();
  return (
    <FormGroup
      label={t("subject")}
      fieldId="kc-subject"
      labelIcon={
        <HelpItem
          helpText="clients-help:subject"
          forLabel={t("subject")}
          forID="kc-subject"
        />
      }
    >
      <TextInput
        ref={register()}
        type="text"
        id="kc-subject"
        name="attributes.x509-subjectdn"
      />
    </FormGroup>
  );
};
