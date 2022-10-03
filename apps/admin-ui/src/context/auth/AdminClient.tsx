import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import axios from "axios";
import Keycloak from "keycloak-js";
import { DependencyList, useEffect } from "react";
import { useErrorHandler } from "react-error-boundary";

import environment from "../../environment";
import { createNamedContext } from "../../utils/createNamedContext";
import useRequiredContext from "../../utils/useRequiredContext";

export type AdminClientProps = {
  keycloak: Keycloak;
  adminClient: KeycloakAdminClient;
};

export const AdminClientContext = createNamedContext<
  AdminClientProps | undefined
>("AdminClientContext", undefined);

export const useAdminClient = () => useRequiredContext(AdminClientContext);

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
  const { adminClient } = useAdminClient();
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

function getKeycloakConfig() {
  if (environment.isRunningAsTheme) {
    return environment.consoleBaseUrl + "config";
  }

  return {
    url: environment.authServerUrl,
    realm: environment.loginRealm,
    clientId: "security-admin-console-v2",
  };
}

export async function initAdminClient() {
  const config = getKeycloakConfig();
  const keycloak = new Keycloak(config);

  await keycloak.init({ onLoad: "check-sso", pkceMethod: "S256" });

  const adminClient = new KeycloakAdminClient();

  adminClient.setConfig({ realmName: environment.loginRealm });
  adminClient.baseUrl = environment.authUrl;
  adminClient.registerTokenProvider({
    async getAccessToken() {
      try {
        await keycloak.updateToken(5);
      } catch (error) {
        keycloak.login();
      }

      return keycloak.token;
    },
  });

  return { keycloak, adminClient };
}
