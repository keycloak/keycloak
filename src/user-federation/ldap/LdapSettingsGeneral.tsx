import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React, { useState } from "react";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { UseFormMethods, Controller } from "react-hook-form";
import { FormAccess } from "../../components/form-access/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";

import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";

export type LdapSettingsGeneralProps = {
  form: UseFormMethods;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
};

export const LdapSettingsGeneral = ({
  form,
  showSectionHeading = false,
  showSectionDescription = false,
}: LdapSettingsGeneralProps) => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;
  const { realm } = useRealm();

  const [isVendorDropdownOpen, setIsVendorDropdownOpen] = useState(false);

  return (
    <>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("generalOptions")}
          description={helpText("ldapGeneralOptionsSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}
      <FormAccess role="manage-realm" isHorizontal>
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
          {/* These hidden fields are required so data object written back matches data retrieved */}
          <TextInput
            hidden
            type="text"
            id="kc-console-provider-id"
            name="providerId"
            defaultValue="ldap"
            ref={form.register}
          />
          <TextInput
            hidden
            type="text"
            id="kc-console-provider-type"
            name="providerType"
            defaultValue="org.keycloak.storage.UserStorageProvider"
            ref={form.register}
          />
          <TextInput
            hidden
            type="text"
            id="kc-console-parentId"
            name="parentId"
            defaultValue={realm}
            ref={form.register}
          />
          <TextInput
            isRequired
            type="text"
            id="kc-console-display-name"
            name="name"
            defaultValue="ldap"
            ref={form.register({
              required: {
                value: true,
                message: `${t("validateName")}`,
              },
            })}
            data-testid="ldap-name"
          />
          {form.errors.name && (
            <div className="error">{form.errors.name.message}</div>
          )}
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
            name="config.vendor[0]"
            defaultValue=""
            control={form.control}
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
                // data-testid="ldap-vendor"
              >
                <SelectOption key={0} value="ad" isPlaceholder>
                  Active Directory
                </SelectOption>
                <SelectOption key={1} value="rhds">
                  Red Hat Directory Server
                </SelectOption>
                <SelectOption key={2} value="tivoli">
                  Tivoli
                </SelectOption>
                <SelectOption key={3} value="edirectory">
                  Novell eDirectory
                </SelectOption>
                <SelectOption key={4} value="other">
                  Other
                </SelectOption>
              </Select>
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </>
  );
};
