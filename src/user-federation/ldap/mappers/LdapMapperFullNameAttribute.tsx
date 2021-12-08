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
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      <FormGroup
        label={t("ldapFullNameAttribute")}
        labelIcon={
          <HelpItem
            helpText={helpText("ldapFullNameAttributeHelp")}
            forLabel={t("ldapFullNameAttribute")}
            forID="kc-full-name-attribute"
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
            helpText={helpText("fullNameLdapReadOnlyHelp")}
            forLabel={t("readOnly")}
            forID="kc-read-only"
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
            helpText={helpText("fullNameLdapWriteOnlyHelp")}
            forLabel={t("writeOnly")}
            forID="kc-write-only"
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
