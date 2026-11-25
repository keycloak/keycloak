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
  const [profile, setProfile] = useState<{
    config: UserProfileConfig;
    refreshCount: number;
  }>();
  const [refreshCount, setRefreshCount] = useState(0);
  const [pendingSaves, setPendingSaves] = useState(0);

  useFetch(
    () => adminClient.users.getProfile({ realm }),
    (config) => setProfile({ config, refreshCount }),
    [realm, refreshCount],
  );

  const save: SaveCallback = async (updatedConfig, options) => {
    setPendingSaves((count) => count + 1);

    try {
      await adminClient.users.updateProfile({
        ...updatedConfig,
        realm,
      });

      // Reload the saved profile because PUT responses may arrive out of order.
      setRefreshCount((count) => count + 1);
      addAlert(
        t(options?.successMessageKey ?? "userProfileSuccess"),
        AlertVariant.success,
      );

      return true;
    } catch (error) {
      addError(options?.errorMessageKey ?? "userProfileError", error);

      return false;
    } finally {
      setPendingSaves((count) => count - 1);
    }
  };

  const config = profile?.config ?? null;
  const isSaving =
    pendingSaves > 0 || refreshCount !== (profile?.refreshCount ?? 0);

  return (
    <UserProfileContext.Provider value={{ config, save, isSaving }}>
      {children}
    </UserProfileContext.Provider>
  );
};

export const useUserProfile = () => useRequiredContext(UserProfileContext);
