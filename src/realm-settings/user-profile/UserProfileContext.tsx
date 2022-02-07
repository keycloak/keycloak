import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { AlertVariant } from "@patternfly/react-core";
import React, { createContext, FunctionComponent, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAlerts } from "../../components/alert/Alerts";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import useRequiredContext from "../../utils/useRequiredContext";

type UserProfileProps = {
  config: UserProfileConfig | null;
  save: SaveCallback;
  isSaving: boolean;
};

export type SaveCallback = (
  updatedConfig: UserProfileConfig,
  options?: SaveOptions
) => Promise<void>;

export type SaveOptions = {
  successMessageKey?: string;
  errorMessageKey?: string;
};

export const UserProfile = createContext<UserProfileProps | undefined>(
  undefined
);

export const UserProfileProvider: FunctionComponent = ({ children }) => {
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();
  const [config, setConfig] = useState<UserProfileConfig | null>(null);
  const [refreshCount, setRefreshCount] = useState(0);
  const [isSaving, setIsSaving] = useState(false);

  useFetch(
    () => adminClient.users.getProfile({ realm }),
    (config) => setConfig(config),
    [refreshCount]
  );

  const save: SaveCallback = async (updatedConfig, options) => {
    setIsSaving(true);

    try {
      await adminClient.users.updateProfile({
        ...updatedConfig,
        realm,
      });

      setRefreshCount(refreshCount + 1);
      addAlert(
        t(options?.successMessageKey ?? "realm-settings:userProfileSuccess"),
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
    <UserProfile.Provider value={{ config, save, isSaving }}>
      {children}
    </UserProfile.Provider>
  );
};

export const useUserProfile = () => useRequiredContext(UserProfile);
