import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import { FormGroup } from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { useState } from "react";
import { Controller, FormProvider, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "@keycloak/keycloak-ui-shared";
import { useAdminClient } from "../../admin-client";
import { FormAccess } from "../../components/form/FormAccess";
import { WizardSectionHeader } from "../../components/wizard-section-header/WizardSectionHeader";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useFetch } from "../../utils/useFetch";

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
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { realm } = useRealm();

  useFetch(
    () => adminClient.realms.findOne({ realm }),
    (result) => form.setValue("parentId", result!.id),
    [],
  );
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
              <Select
                isDisabled={!!vendorEdit}
                toggleId="kc-vendor"
                required
                onToggle={() => setIsVendorDropdownOpen(!isVendorDropdownOpen)}
                isOpen={isVendorDropdownOpen}
                onSelect={(_, value) => {
                  field.onChange(value as string);
                  setIsVendorDropdownOpen(false);
                  setVendorDefaultValues();
                }}
                selections={field.value}
                variant={SelectVariant.single}
                aria-label={t("selectVendor")}
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
    </FormProvider>
  );
};
