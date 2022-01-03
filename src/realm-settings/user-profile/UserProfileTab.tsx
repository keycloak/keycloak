import type ClientProfilesRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientProfilesRepresentation";
import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { AlertVariant, Tab, Tabs, TabTitleText } from "@patternfly/react-core";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAlerts } from "../../components/alert/Alerts";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { JsonEditorTab } from "./JsonEditorTab";

export const UserProfileTab = () => {
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { t } = useTranslation("realm-settings");
  const { addAlert, addError } = useAlerts();
  const [activeTab, setActiveTab] = useState("attributes");
  const [config, setConfig] = useState<UserProfileConfig>();
  const [isSaving, setIsSaving] = useState(false);
  const [refreshCount, setRefreshCount] = useState(0);

  useFetch(
    () => adminClient.users.getProfile({ realm }),
    (config) => setConfig(config),
    [refreshCount]
  );

  async function onSave(updatedProfiles: ClientProfilesRepresentation) {
    setIsSaving(true);

    try {
      await adminClient.clientPolicies.createProfiles({
        ...updatedProfiles,
        realm,
      });

      setRefreshCount(refreshCount + 1);
      addAlert(t("userProfileSuccess"), AlertVariant.success);
    } catch (error) {
      addError("realm-settings:userProfileError", error);
    }

    setIsSaving(false);
  }

  return (
    <Tabs
      activeKey={activeTab}
      onSelect={(_, key) => setActiveTab(key.toString())}
      mountOnEnter
    >
      <Tab
        eventKey="attributes"
        title={<TabTitleText>{t("attributes")}</TabTitleText>}
      ></Tab>
      <Tab
        eventKey="attributesGroup"
        title={<TabTitleText>{t("attributesGroup")}</TabTitleText>}
      ></Tab>
      <Tab
        eventKey="jsonEditor"
        title={<TabTitleText>{t("jsonEditor")}</TabTitleText>}
      >
        <JsonEditorTab config={config} onSave={onSave} isSaving={isSaving} />
      </Tab>
    </Tabs>
  );
};
