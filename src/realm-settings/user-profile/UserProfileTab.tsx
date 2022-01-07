import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { AlertVariant, Tab, Tabs, TabTitleText } from "@patternfly/react-core";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAlerts } from "../../components/alert/Alerts";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { AttributesGroupTab } from "./AttributesGroupTab";
import { JsonEditorTab } from "./JsonEditorTab";

export type OnSaveCallback = (
  updatedProfiles: UserProfileConfig,
  options?: OnSaveOptions
) => Promise<void>;

export type OnSaveOptions = {
  successMessageKey?: string;
  errorMessageKey?: string;
};

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

  const onSave: OnSaveCallback = async (
    updatedProfiles: UserProfileConfig,
    options?: OnSaveOptions
  ) => {
    setIsSaving(true);

    try {
      await adminClient.users.updateProfile({
        ...updatedProfiles,
        realm,
      });

      setRefreshCount(refreshCount + 1);
      addAlert(
        t(options?.successMessageKey ?? "userProfileSuccess"),
        AlertVariant.success
      );
    } catch (error) {
      addError(
        options?.errorMessageKey ?? "realm-settings:userProfileError",
        error
      );
    }

    setIsSaving(false);
  };

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
        data-testid="attributesGroupTab"
      >
        <AttributesGroupTab config={config} onSave={onSave} />
      </Tab>
      <Tab
        eventKey="jsonEditor"
        title={<TabTitleText>{t("jsonEditor")}</TabTitleText>}
      >
        <JsonEditorTab config={config} onSave={onSave} isSaving={isSaving} />
      </Tab>
    </Tabs>
  );
};
