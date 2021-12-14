import { FormGroup, Switch } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { Controller, UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";

export type LdapMapperMsadUserAccountProps = {
  form: UseFormMethods;
};

export const LdapMapperMsadUserAccount = ({
  form,
}: LdapMapperMsadUserAccountProps) => {
  const { t } = useTranslation("user-federation");

  return (
    <FormGroup
      label={t("passwordPolicyHintsEnabled")}
      labelIcon={
        <HelpItem
          helpText="user-federation-help:passwordPolicyHintsEnabledHelp"
          fieldLabelId="user-federation:passwordPolicyHintsEnabled"
        />
      }
      fieldId="kc-der-formatted"
      hasNoPaddingTop
    >
      <Controller
        name="config.ldap.password.policy.hints.enabled"
        defaultValue={["false"]}
        control={form.control}
        render={({ onChange, value }) => (
          <Switch
            id={"kc-pw-policy-hints-enabled"}
            isDisabled={false}
            onChange={(value) => onChange([`${value}`])}
            isChecked={value[0] === "true"}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        )}
      ></Controller>
    </FormGroup>
  );
};
