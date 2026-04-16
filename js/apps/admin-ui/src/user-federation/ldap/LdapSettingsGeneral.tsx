import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
  TextControl,
} from "@keycloak/keycloak-ui-shared";
import { FormGroup, SelectOption } from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, FormProvider, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../components/form/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { useRealm } from "../../context/realm-context/RealmContext";

export type LdapSettingsGeneralProps = {
  form: UseFormReturn<ComponentRepresentation>;
  showSectionHeading?: boolean;
  showSectionDescription?: boolean;
  vendorEdit?: boolean;
};

export const LdapSettingsGeneral = ({
  form,
  showSectionHeading = false,
  showSectionDescription = false,
  vendorEdit = false,
}: LdapSettingsGeneralProps) => {
  const { t } = useTranslation();
  const { realm, realmRepresentation } = useRealm();

  useEffect(() => form.setValue("parentId", realmRepresentation?.id), []);
  const [isVendorDropdownOpen, setIsVendorDropdownOpen] = useState(false);

  const setVendorDefaultValues = () => {
    switch (form.getValues("config.vendor[0]")) {
      case "ad":
        form.setValue("config.usernameLDAPAttribute[0]", "cn");
        form.setValue("config.rdnLDAPAttribute[0]", "cn");
        form.setValue("config.uuidLDAPAttribute[0]", "objectGUID");
        form.setValue("config.krbPrincipalAttribute[0]", "userPrincipalName");
        form.setValue(
          "config.userObjectClasses[0]",
          "person, organizationalPerson, user",
        );
        break;
      case "rhds":
        form.setValue("config.usernameLDAPAttribute[0]", "uid");
        form.setValue("config.rdnLDAPAttribute[0]", "uid");
        form.setValue("config.uuidLDAPAttribute[0]", "nsuniqueid");
        form.setValue("config.krbPrincipalAttribute[0]", "krbPrincipalName");
        form.setValue(
          "config.userObjectClasses[0]",
          "inetOrgPerson, organizationalPerson",
        );
        break;
      case "tivoli":
        form.setValue("config.usernameLDAPAttribute[0]", "uid");
        form.setValue("config.rdnLDAPAttribute[0]", "uid");
        form.setValue("config.uuidLDAPAttribute[0]", "uniqueidentifier");
        form.setValue("config.krbPrincipalAttribute[0]", "krb5PrincipalName");
        form.setValue(
          "config.userObjectClasses[0]",
          "inetOrgPerson, organizationalPerson",
        );
        break;
      case "edirectory":
        form.setValue("config.usernameLDAPAttribute[0]", "uid");
        form.setValue("config.rdnLDAPAttribute[0]", "uid");
        form.setValue("config.uuidLDAPAttribute[0]", "guid");
        form.setValue("config.krbPrincipalAttribute[0]", "krb5PrincipalName");
        form.setValue(
          "config.userObjectClasses[0]",
          "inetOrgPerson, organizationalPerson",
        );
        break;
      case "other":
        form.setValue("config.usernameLDAPAttribute[0]", "uid");
        form.setValue("config.rdnLDAPAttribute[0]", "uid");
        form.setValue("config.uuidLDAPAttribute[0]", "entryUUID");
        form.setValue("config.krbPrincipalAttribute[0]", "krb5PrincipalName");
        form.setValue(
          "config.userObjectClasses[0]",
          "inetOrgPerson, organizationalPerson",
        );
        break;
      default:
        return "";
    }
  };

  return (
    <FormProvider {...form}>
      {showSectionHeading && (
        <WizardSectionHeader
          title={t("generalOptions")}
          description={t("ldapGeneralOptionsSettingsDescription")}
          showDescription={showSectionDescription}
        />
      )}
      <FormAccess role="manage-realm" isHorizontal>
        {/* These hidden fields are required so data object written back matches data retrieved */}
        <input
          type="hidden"
          defaultValue="ldap"
          {...form.register("providerId")}
        />
        <input
          type="hidden"
          defaultValue="org.keycloak.storage.UserStorageProvider"
          {...form.register("providerType")}
        />
        <input
          type="hidden"
          defaultValue={realm}
          {...form.register("parentId")}
        />
        <TextControl
          name="name"
          label={t("uiDisplayName")}
          labelIcon={t("uiDisplayNameHelp")}
          defaultValue="ldap"
          rules={{
            required: t("validateName"),
          }}
        />
        <FormGroup
          label={t("vendor")}
          labelIcon={
            <HelpItem helpText={t("vendorHelp")} fieldLabelId="vendor" />
          }
          fieldId="kc-vendor"
          isRequired
        >
          <Controller
            name="config.vendor[0]"
            defaultValue="ad"
            control={form.control}
            render={({ field }) => (
              <KeycloakSelect
                isDisabled={vendorEdit}
                toggleId="kc-vendor"
                onToggle={() => setIsVendorDropdownOpen(!isVendorDropdownOpen)}
                isOpen={isVendorDropdownOpen}
                onSelect={(value) => {
                  field.onChange(value as string);
                  setIsVendorDropdownOpen(false);
                  setVendorDefaultValues();
                }}
                selections={field.value}
                variant={SelectVariant.single}
                aria-label={t("selectVendor")}
              >
                <SelectOption key={0} value="ad">
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
              </KeycloakSelect>
            )}
          ></Controller>
        </FormGroup>
      </FormAccess>
    </FormProvider>
  );
};
