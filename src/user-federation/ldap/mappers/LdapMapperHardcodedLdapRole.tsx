import { FormGroup, TextInput } from "@patternfly/react-core";
import React from "react";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { UseFormMethods } from "react-hook-form";
import { FormAccess } from "../../../components/form-access/FormAccess";
import { useTranslation } from "react-i18next";
import { LdapMapperGeneral } from "./shared/LdapMapperGeneral";

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
      <FormAccess role="manage-realm" isHorizontal>
        <LdapMapperGeneral form={form} />
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
            name="config.role"
            ref={form.register}
          />
        </FormGroup>
      </FormAccess>
    </>
  );
};
