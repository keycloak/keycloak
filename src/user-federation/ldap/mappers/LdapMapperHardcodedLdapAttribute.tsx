import { FormGroup, TextInput } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { UseFormMethods } from "react-hook-form";
import { FormAccess } from "../../../components/form-access/FormAccess";
import { useTranslation } from "react-i18next";
import { LdapMapperGeneral } from "./shared/LdapMapperGeneral";

export type LdapMapperHardcodedLdapAttributeProps = {
  form: UseFormMethods;
};

export const LdapMapperHardcodedLdapAttribute = ({
  form,
}: LdapMapperHardcodedLdapAttributeProps) => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      <FormAccess role="manage-realm" isHorizontal>
        <LdapMapperGeneral form={form} />

        <FormGroup
          label={t("ldapAttributeName")}
          labelIcon={
            <HelpItem
              helpText={helpText("ldapAttributeNameHelp")}
              forLabel={t("ldapAttributeName")}
              forID="kc-ldap-attribute-name"
            />
          }
          fieldId="kc-ldap-attribute-name"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-ldap-attribute-name"
            data-testid="ldap-attribute-name"
            name="config.ldap-attribute-name"
            ref={form.register}
          />
        </FormGroup>
        <FormGroup
          label={t("ldapAttributeValue")}
          labelIcon={
            <HelpItem
              helpText={helpText("ldapAttributeValueHelp")}
              forLabel={t("ldapAttributeValue")}
              forID="kc-ldap-attribute-value"
            />
          }
          fieldId="kc-ldap-attribute-value"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-ldap-attribute-value"
            data-testid="ldap-attribute-value"
            name="config.ldap-attribute-value"
            ref={form.register}
          />
        </FormGroup>
      </FormAccess>
    </>
  );
};
