import { FormGroup, TextInput } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { UseFormMethods } from "react-hook-form";
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
          data-testid="group"
          name="config.group[0]"
          ref={form.register}
        />
      </FormGroup>
    </>
  );
};
