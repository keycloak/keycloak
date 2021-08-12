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
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      <FormGroup
        label={t("group")}
        labelIcon={
          <HelpItem
            helpText={helpText("groupHelp")}
            forLabel={t("group")}
            forID="kc-group"
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
    </>
  );
};
