import React from "react";
import { PageSection, Tab, TabTitleText } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { KeycloakTabs } from "../components/keycloak-tabs/KeycloakTabs";
import { RealmSettingsLoginTab } from "./LoginTab";
import { RealmSettingsGeneralTab } from "./GeneralTab";

export const RealmSettingsTabs = () => {
  const { t } = useTranslation("roles");

  return (
    <>
      <PageSection variant="light" className="pf-u-p-0">
        <KeycloakTabs isBox>
          <Tab
            eventKey="general"
            title={<TabTitleText>{t("realm-settings:general")}</TabTitleText>}
          >
            <RealmSettingsGeneralTab />
          </Tab>
          <Tab
            eventKey="login"
            title={<TabTitleText>{t("realm-settings:login")}</TabTitleText>}
          >
            <RealmSettingsLoginTab />
          </Tab>
        </KeycloakTabs>
      </PageSection>
    </>
  );
};
