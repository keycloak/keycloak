import { FormGroup, Switch, TextInput } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { Controller, UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";

export type LdapMapperFullNameAttributeProps = {
  form: UseFormMethods;
};

export const LdapMapperFullNameAttribute = ({
  form,
}: LdapMapperFullNameAttributeProps) => {
  const { t } = useTranslation("user-federation");

  return (
    <>
      <FormGroup
        label={t("ldapFullNameAttribute")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:ldapFullNameAttributeHelp"
            fieldLabelId="user-federation:ldapFullNameAttribute"
          />
        }
        fieldId="kc-full-name-attribute"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          defaultValue="cn"
          id="kc-full-name-attribute"
          data-testid="mapper-fullNameAttribute-fld"
          name="config.ldap.full.name.attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("readOnly")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:fullNameLdapReadOnlyHelp"
            fieldLabelId="user-federation:readOnly"
          />
        }
        fieldId="kc-read-only"
        hasNoPaddingTop
      >
        <Controller
          name="config.read.only"
          defaultValue={["true"]}
          control={form.control}
          render={({ onChange, value }) => (
            <Switch
              id={"kc-read-only"}
              isDisabled={false}
              onChange={(value) => onChange([`${value}`])}
              isChecked={value[0] === "true"}
              label={t("common:on")}
              labelOff={t("common:off")}
            />
          )}
        ></Controller>
      </FormGroup>
      <FormGroup
        label={t("writeOnly")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:fullNameLdapWriteOnlyHelp"
            fieldLabelId="user-federation:writeOnly"
          />
        }
        fieldId="kc-read-only"
        hasNoPaddingTop
      >
        <Controller
          name="config.write.only"
          defaultValue={["false"]}
          control={form.control}
          render={({ onChange, value }) => (
            <Switch
              id={"kc-write-only"}
              isDisabled={false}
              onChange={(value) => onChange([`${value}`])}
              isChecked={value[0] === "true"}
              label={t("common:on")}
              labelOff={t("common:off")}
            />
          )}
        ></Controller>
      </FormGroup>
    </>
  );
};
