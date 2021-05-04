import { FormGroup, TextInput } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import type { UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";

export type LdapMapperHardcodedLdapRoleProps = {
  form: UseFormMethods;
};

export const LdapMapperHardcodedLdapRole = ({
  form,
}: LdapMapperHardcodedLdapRoleProps) => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      <FormGroup
        label={t("common:role")}
        labelIcon={
          <HelpItem
            helpText={helpText("roleHelp")}
            forLabel={t("common:role")}
            forID="kc-role"
          />
        }
        fieldId="kc-role"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-role"
          data-testid="role"
          name="config.role[0]"
          ref={form.register}
        />
      </FormGroup>
    </>
  );
};
