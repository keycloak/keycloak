import { Form, FormGroup, Switch } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";

export const LdapSettingsAdvanced = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      <Form isHorizontal>
        <FormGroup
          label={t("enableLdapv3Password")}
          labelIcon={
            <HelpItem
              helpText={helpText("enableLdapv3PasswordHelp")}
              forLabel={t("enableLdapv3Password")}
              forID="kc-enable-ldapv3-password"
            />
          }
          fieldId="kc-enable-ldapv3-password"
          hasNoPaddingTop
        >
          <Switch
            id={"kc-enable-ldapv3-password"}
            isChecked={false}
            isDisabled={false}
            onChange={() => undefined as any}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        </FormGroup>

        <FormGroup
          label={t("validatePasswordPolicy")}
          labelIcon={
            <HelpItem
              helpText={helpText("validatePasswordPolicyHelp")}
              forLabel={t("validatePasswordPolicy")}
              forID="kc-validate-password-policy"
            />
          }
          fieldId="kc-validate-password-policy"
          hasNoPaddingTop
        >
          <Switch
            id={"kc-validate-password-policy"}
            isChecked={false}
            isDisabled={false}
            onChange={() => undefined as any}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        </FormGroup>

        <FormGroup
          label={t("trustEmail")}
          labelIcon={
            <HelpItem
              helpText={helpText("trustEmailHelp")}
              forLabel={t("trustEmail")}
              forID="kc-trust-email"
            />
          }
          fieldId="kc-trust-email"
          hasNoPaddingTop
        >
          <Switch
            id={"kc-trust-email"}
            isChecked={false}
            isDisabled={false}
            onChange={() => undefined as any}
            label={t("common:on")}
            labelOff={t("common:off")}
          />
        </FormGroup>
      </Form>
    </>
  );
};
