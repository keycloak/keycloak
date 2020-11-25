import { FormGroup, Switch } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useForm, Controller } from "react-hook-form";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";
import { FormAccess } from "../components/form-access/FormAccess";

export const LdapSettingsAdvanced = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const { control } = useForm<ComponentRepresentation>();

  return (
    <>
      <FormAccess role="manage-realm" isHorizontal>
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
          <Controller
            name="enableLadpv3PasswordModify"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-enable-ldapv3-password"}
                isChecked={value}
                isDisabled={false}
                onChange={onChange}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
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
          <Controller
            name="validatePasswordPolicy"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-validate-password-policy"}
                isChecked={value}
                isDisabled={false}
                onChange={onChange}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
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
          <Controller
            name="trustEmail"
            defaultValue={false}
            control={control}
            render={({ onChange, value }) => (
              <Switch
                id={"kc-trust-email"}
                isChecked={value}
                isDisabled={false}
                onChange={onChange}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </>
  );
};
