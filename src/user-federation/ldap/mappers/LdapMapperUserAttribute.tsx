import { FormGroup, Switch, TextInput } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { Controller, UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";

export type LdapMapperUserAttributeProps = {
  form: UseFormMethods;
  mapperType: string | undefined;
};

export const LdapMapperUserAttribute = ({
  form,
  mapperType,
}: LdapMapperUserAttributeProps) => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      <FormGroup
        label={t("userModelAttribute")}
        labelIcon={
          <HelpItem
            helpText={helpText("userModelAttributeHelp")}
            forLabel={t("userModelAttribute")}
            forID="kc-user-model-attribute"
          />
        }
        fieldId="kc-user-model-attribute"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-user-model-attribute"
          data-testid="mapper-userModelAttribute-fld"
          name="config.user.model.attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("ldapAttribute")}
        labelIcon={
          <HelpItem
            helpText={helpText("ldapAttributeHelp")}
            forLabel={t("ldapAttribute")}
            forID="kc-ldap-attribute"
          />
        }
        fieldId="kc-ldap-attribute"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-ldap-attribute"
          data-testid="mapper-ldapAttribute-fld"
          name="config.ldap.attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("readOnly")}
        labelIcon={
          <HelpItem
            helpText={helpText("readOnlyHelp")}
            forLabel={t("readOnly")}
            forID="kc-read-only"
          />
        }
        fieldId="kc-read-only"
        hasNoPaddingTop
      >
        <Controller
          name="config.read.only"
          defaultValue={
            mapperType === "user-attribute-ldap-mapper" ? ["true"] : ["false"]
          }
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
        label={t("alwaysReadValueFromLdap")}
        labelIcon={
          <HelpItem
            helpText={helpText("alwaysReadValueFromLdapHelp")}
            forLabel={t("alwaysReadValueFromLdap")}
            forID="kc-always-read-value"
          />
        }
        fieldId="kc-always-read-value"
        hasNoPaddingTop
      >
        <Controller
          name="config.always.read.value.from.ldap"
          defaultValue={["false"]}
          control={form.control}
          render={({ onChange, value }) => (
            <Switch
              id={"kc-always-read-value"}
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
        label={t("isMandatoryInLdap")}
        labelIcon={
          <HelpItem
            helpText={helpText("isMandatoryInLdapHelp")}
            forLabel={t("isMandatoryInLdap")}
            forID="kc-is-mandatory"
          />
        }
        fieldId="kc-is-mandatory"
        hasNoPaddingTop
      >
        <Controller
          name="config.is.mandatory.in.ldap"
          defaultValue={["false"]}
          control={form.control}
          render={({ onChange, value }) => (
            <Switch
              id={"kc-is-mandatory"}
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
        label={t("attributeDefaultValue")}
        labelIcon={
          <HelpItem
            helpText={helpText("attributeDefaultValueHelp")}
            forLabel={t("attributeDefaultValue")}
            forID="kc-attribute-default-value"
          />
        }
        fieldId="kc-attribute-default-value"
      >
        <TextInput
          type="text"
          id="kc-attribute-default-value"
          data-testid="mapper-attributeDefaultValue-fld"
          name="config.attribute.default.value[0]"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("isBinaryAttribute")}
        labelIcon={
          <HelpItem
            helpText={helpText("isBinaryAttributeHelp")}
            forLabel={t("isBinaryAttribute")}
            forID="kc-is-binary"
          />
        }
        fieldId="kc-is-binary"
        hasNoPaddingTop
      >
        <Controller
          name="config.is.binary.attribute"
          defaultValue={["false"]}
          control={form.control}
          render={({ onChange, value }) => (
            <Switch
              id={"kc-is-binary"}
              isDisabled={false}
              onChange={(value) => onChange([`${value}`])}
              isChecked={value[0] === "true"}
              label={t("common:on")}
              labelOff={t("common:off")}
            />
          )}
        ></Controller>
      </FormGroup>
      {mapperType === "certificate-ldap-mapper" ? (
        <FormGroup
          label={t("derFormatted")}
          labelIcon={
            <HelpItem
              helpText={helpText("derFormattedHelp")}
              forLabel={t("derFormatted")}
              forID="kc-der-formatted"
            />
          }
          fieldId="kc-der-formatted"
          hasNoPaddingTop
        >
          <Controller
            name="config.is.der.formatted"
            defaultValue={["false"]}
            control={form.control}
            render={({ onChange, value }) => (
              <Switch
                id="kc-der-formatted"
                isDisabled={false}
                onChange={(value) => onChange([`${value}`])}
                isChecked={value[0] === "true"}
                label={t("common:on")}
                labelOff={t("common:off")}
              />
            )}
          ></Controller>
        </FormGroup>
      ) : null}
    </>
  );
};
