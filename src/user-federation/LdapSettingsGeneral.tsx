import {
  Form,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useState } from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";
import { useForm, Controller } from "react-hook-form";
import ComponentRepresentation from "keycloak-admin/lib/defs/componentRepresentation";

export const LdapSettingsGeneral = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  const [isVendorDropdownOpen, setIsVendorDropdownOpen] = useState(false);
  const { register, handleSubmit, control } = useForm<
    ComponentRepresentation
  >();
  const onSubmit = (data: ComponentRepresentation) => {
    console.log(data);
  };

  return (
    <>
      {/* Cache settings */}
      <Form isHorizontal onSubmit={handleSubmit(onSubmit)}>
        <FormGroup
          label={t("consoleDisplayName")}
          labelIcon={
            <HelpItem
              helpText={helpText("consoleDisplayNameHelp")}
              forLabel={t("consoleDisplayName")}
              forID="kc-console-display-name"
            />
          }
          fieldId="kc-console-display-name"
          isRequired
        >
          <TextInput
            isRequired
            type="text"
            id="kc-console-display-name"
            name="displayName"
            ref={register}
          />
        </FormGroup>

        <FormGroup
          label={t("vendor")}
          labelIcon={
            <HelpItem
              helpText={helpText("vendorHelp")}
              forLabel={t("vendor")}
              forID="kc-vendor"
            />
          }
          fieldId="kc-vendor"
          isRequired
        >
          <Controller
            name="vendor"
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="kc-vendor"
                required
                onToggle={() => setIsVendorDropdownOpen(!isVendorDropdownOpen)}
                isOpen={isVendorDropdownOpen}
                onSelect={(_, value) => {
                  onChange(value as string);
                  setIsVendorDropdownOpen(false);
                }}
                selections={value}
                variant={SelectVariant.single}
                // aria-label="Other"
                // isDisabled
              >
                <SelectOption key={0} value="Choose..." isPlaceholder />
                <SelectOption key={1} value="Active Directory" />
                <SelectOption key={2} value="Red Hat Directory Server" />
                <SelectOption key={3} value="Tivoli" />
                <SelectOption key={4} value="Novell eDirectory" />
                <SelectOption key={5} value="Other" />
              </Select>
            )}
          ></Controller>
        </FormGroup>
        <button type="submit">Test submit</button>
      </Form>
    </>
  );
};
