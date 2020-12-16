import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { LdapSettingsAdvanced } from "./ldap/LdapSettingsAdvanced";
import { LdapSettingsKerberosIntegration } from "./ldap/LdapSettingsKerberosIntegration";
import { LdapSettingsCache } from "./ldap/LdapSettingsCache";
import { LdapSettingsSynchronization } from "./ldap/LdapSettingsSynchronization";
import { LdapSettingsGeneral } from "./ldap/LdapSettingsGeneral";
import { LdapSettingsConnection } from "./ldap/LdapSettingsConnection";
import { LdapSettingsSearching } from "./ldap/LdapSettingsSearching";

export const UserFederationLdapSettings = () => {
  const { t } = useTranslation("user-federation");

  return (
    <>
      <PageSection variant="light" isFilled>
        <ScrollForm
          sections={[
            t("generalOptions"),
            t("connectionAndAuthenticationSettings"),
            t("ldapSearchingAndUpdatingSettings"),
            t("synchronizationSettings"),
            t("kerberosIntegration"),
            t("cacheSettings"),
            t("advancedSettings"),
          ]}
        >
          {/* General settings */}
          <LdapSettingsGeneral />

          {/* Connection settings */}
          <LdapSettingsConnection />

          {/* Searching and updating settings */}
          <LdapSettingsSearching />

          {/* Synchronization settings */}
          <LdapSettingsSynchronization />

          {/* Kerberos integration */}
          <LdapSettingsKerberosIntegration />

          {/* Cache settings */}
          <LdapSettingsCache />

          {/* Advanced settings */}
          <LdapSettingsAdvanced />
        </ScrollForm>
      </PageSection>
    </>
  );
};
