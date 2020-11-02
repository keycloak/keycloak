import React from "react";
import { useTranslation } from "react-i18next";
import { UseFormMethods } from "react-hook-form";
import { FormGroup, TextInput } from "@patternfly/react-core";
import { HelpItem } from "../../components/help-enabler/HelpItem";

export type X509Props = {
  form: UseFormMethods;
};

export const X509 = ({ form }: X509Props) => {
  const { t } = useTranslation("clients");
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
        ref={form.register()}
        type="text"
        id="kc-subject"
        name="attributes.x509_subjectdn"
      />
    </FormGroup>
  );
};
