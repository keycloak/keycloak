import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import axios from "axios";
import { createContext, DependencyList, useEffect } from "react";
import { useErrorHandler } from "react-error-boundary";

import environment from "../../environment";
import useRequiredContext from "../../utils/useRequiredContext";

export const AdminClient = createContext<KeycloakAdminClient | undefined>(
  undefined
);

export const useAdminClient = () => useRequiredContext(AdminClient);

/**
 * Util function to only set the state when the component is still mounted.
 *
 * It takes 2 functions one you do your adminClient call in and the other to set your state
 *
 * @example
 * useFetch(
 *  () => adminClient.components.findOne({ id }),
 *  (component) => setupForm(component),
 *  []
 * );
 *
 * @param adminClientCall use this to do your adminClient call
 * @param callback when the data is fetched this is where you set your state
 */
export function useFetch<T>(
  adminClientCall: () => Promise<T>,
  callback: (param: T) => void,
  deps?: DependencyList
) {
  const adminClient = useAdminClient();
  const onError = useErrorHandler();

  useEffect(() => {
    const source = axios.CancelToken.source();

    adminClient.setConfig({
      requestConfig: { cancelToken: source.token },
    });

    adminClientCall()
      .then((result) => {
        if (!source.token.reason) {
          callback(result);
        }
      })
      .catch((error) => {
        if (!axios.isCancel(error)) {
          onError(error);
        }
      });

    adminClient.setConfig({
      requestConfig: { cancelToken: undefined },
    });

    return () => {
      source.cancel();
    };
  }, deps);
}

export async function initAdminClient() {
  const kcAdminClient = new KeycloakAdminClient();

  await kcAdminClient.init(
    { onLoad: "check-sso", pkceMethod: "S256" },
    {
      url: environment.authServerUrl,
      realm: environment.loginRealm,
      clientId: environment.isRunningAsTheme
        ? "security-admin-console"
        : "security-admin-console-v2",
    }
  );

  kcAdminClient.setConfig({ realmName: environment.loginRealm });
  kcAdminClient.baseUrl = environment.authServerUrl;

  return kcAdminClient;
}
