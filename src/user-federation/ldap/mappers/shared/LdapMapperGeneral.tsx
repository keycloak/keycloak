import { FormGroup, TextInput } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../../components/help-enabler/HelpItem";
import { UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";

export type LdapMapperGeneralProps = {
  form: UseFormMethods;
};

export const LdapMapperGeneral = ({ form }: LdapMapperGeneralProps) => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      <FormGroup label={t("common:id")} fieldId="kc-ldap-mapper-id">
        <TextInput
          isRequired
          type="text"
          id="kc-ldap-mapper-id"
          data-testid="ldap-mapper-id"
          name="id"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("common:name")}
        labelIcon={
          <HelpItem
            helpText={helpText("nameHelp")}
            forLabel={t("common:name")}
            forID="kc-ldap-mapper-name"
          />
        }
        fieldId="kc-ldap-mapper-name"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-ldap-mapper-name"
          data-testid="ldap-mapper-name"
          name="name"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("common:mapperType")}
        labelIcon={
          <HelpItem
            helpText={helpText("mapperTypeHelp")}
            forLabel={t("common:mapperType")}
            forID="kc-ldap-mapper-type"
          />
        }
        fieldId="kc-ldap-mapper-type"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-ldap-mapper-type"
          data-testid="ldap-mapper-type"
          name="providerId"
          ref={form.register}
        />
      </FormGroup>
    </>
  );
};
