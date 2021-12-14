import { FormGroup, TextInput, ValidatedOptions } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import type { UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";

export type LdapMapperHardcodedLdapGroupProps = {
  form: UseFormMethods;
};

export const LdapMapperHardcodedLdapGroup = ({
  form,
}: LdapMapperHardcodedLdapGroupProps) => {
  const { t } = useTranslation("user-federation");

  return (
    <FormGroup
      label={t("group")}
      labelIcon={
        <HelpItem
          helpText="user-federation-help:groupHelp"
          fieldLabelId="user-federation:group"
        />
      }
      fieldId="kc-group"
      isRequired
    >
      <TextInput
        isRequired
        type="text"
        id="kc-group"
        data-testid="mapper-group-fld"
        name="config.group[0]"
        ref={form.register({ required: true })}
        validated={
          form.errors.config?.group
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
      />
    </FormGroup>
  );
};
