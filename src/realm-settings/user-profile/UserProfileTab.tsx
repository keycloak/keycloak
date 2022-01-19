import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { AlertVariant, Tab, TabTitleText } from "@patternfly/react-core";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { useAlerts } from "../../components/alert/Alerts";
import {
  routableTab,
  RoutableTabs,
} from "../../components/routable-tabs/RoutableTabs";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toUserProfile } from "../routes/UserProfile";
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
  const history = useHistory();
  const { addAlert, addError } = useAlerts();
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
    <RoutableTabs
      defaultLocation={toUserProfile({ realm, tab: "attributes" })}
      mountOnEnter
    >
      <Tab
        title={<TabTitleText>{t("attributes")}</TabTitleText>}
        {...routableTab({
          to: toUserProfile({ realm, tab: "attributes" }),
          history,
        })}
      ></Tab>
      <Tab
        title={<TabTitleText>{t("attributesGroup")}</TabTitleText>}
        data-testid="attributesGroupTab"
        {...routableTab({
          to: toUserProfile({ realm, tab: "attributesGroup" }),
          history,
        })}
      >
        <AttributesGroupTab config={config} onSave={onSave} />
      </Tab>
      <Tab
        title={<TabTitleText>{t("jsonEditor")}</TabTitleText>}
        {...routableTab({
          to: toUserProfile({ realm, tab: "jsonEditor" }),
          history,
        })}
      >
        <JsonEditorTab config={config} onSave={onSave} isSaving={isSaving} />
      </Tab>
    </RoutableTabs>
  );
};
