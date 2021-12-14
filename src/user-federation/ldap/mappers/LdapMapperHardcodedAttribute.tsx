import { FormGroup, TextInput } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import type { UseFormMethods } from "react-hook-form";
import { useTranslation } from "react-i18next";

export type LdapMapperHardcodedAttributeProps = {
  form: UseFormMethods;
};

export const LdapMapperHardcodedAttribute = ({
  form,
}: LdapMapperHardcodedAttributeProps) => {
  const { t } = useTranslation("user-federation");

  return (
    <>
      <FormGroup
        label={t("userModelAttributeName")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:userModelAttributeNameHelp"
            fieldLabelId="user-federation:userModelAttributeName"
          />
        }
        fieldId="kc-user-model-attribute"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-user-model-attribute"
          data-testid="mapper-userModelAttributeName-fld"
          name="config.user.model.attribute[0]"
          ref={form.register}
        />
      </FormGroup>
      <FormGroup
        label={t("attributeValue")}
        labelIcon={
          <HelpItem
            helpText="user-federation-help:attributeValueHelp"
            fieldLabelId="user-federation:attributeValue"
          />
        }
        fieldId="kc-attribute-value"
        isRequired
      >
        <TextInput
          isRequired
          type="text"
          id="kc-attribute-value"
          data-testid="mapper-attributeValue-fld"
          name="config.attribute.value[0]"
          ref={form.register}
        />
      </FormGroup>
    </>
  );
};
