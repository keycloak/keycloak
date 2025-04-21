import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import {
  createNamedContext,
  useAlerts,
  useFetch,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { AlertVariant } from "@patternfly/react-core";
import { PropsWithChildren, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { useRealm } from "../../context/realm-context/RealmContext";

type UserProfileProps = {
  config: UserProfileConfig | null;
  save: SaveCallback;
  isSaving: boolean;
};

export type SaveCallback = (
  updatedConfig: UserProfileConfig,
  options?: SaveOptions,
) => Promise<boolean>;

export type SaveOptions = {
  successMessageKey?: string;
  errorMessageKey?: string;
};

export const UserProfileContext = createNamedContext<
  UserProfileProps | undefined
>("UserProfileContext", undefined);

export const UserProfileProvider = ({ children }: PropsWithChildren) => {
  const { adminClient } = useAdminClient();

  const { realm } = useRealm();
  const { addAlert, addError } = useAlerts();
  const { t } = useTranslation();
  const [config, setConfig] = useState<UserProfileConfig | null>(null);
  const [refreshCount, setRefreshCount] = useState(0);
  const [isSaving, setIsSaving] = useState(false);

  useFetch(
    () => adminClient.users.getProfile({ realm }),
    (config) => setConfig(config),
    [refreshCount],
  );

  const save: SaveCallback = async (updatedConfig, options) => {
    setIsSaving(true);

    try {
      await adminClient.users.updateProfile({
        ...updatedConfig,
        realm,
      });

      setIsSaving(false);
      setRefreshCount(refreshCount + 1);
      addAlert(
        t(options?.successMessageKey ?? "userProfileSuccess"),
        AlertVariant.success,
      );

      return true;
    } catch (error) {
      setIsSaving(false);
      addError(options?.errorMessageKey ?? "userProfileError", error);

      return false;
    }
  };

  return (
    <UserProfileContext.Provider value={{ config, save, isSaving }}>
      {children}
    </UserProfileContext.Provider>
  );
};

export const useUserProfile = () => useRequiredContext(UserProfileContext);
