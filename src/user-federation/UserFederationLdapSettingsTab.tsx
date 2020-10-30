import { PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { ScrollForm } from "../components/scroll-form/ScrollForm";
import { LdapSettingsAdvanced } from "./LdapSettingsAdvanced";
import { LdapSettingsKerberosIntegration } from "./LdapSettingsKerberosIntegration";
import { LdapSettingsCache } from "./LdapSettingsCache";
import { LdapSettingsSynchronization } from "./LdapSettingsSynchronization";
import { LdapSettingsGeneral } from "./LdapSettingsGeneral";
import { LdapSettingsConnection } from "./LdapSettingsConnection";
import { LdapSettingsSearching } from "./LdapSettingsSearching";

export const UserFederationLdapSettingsTab = () => {
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
