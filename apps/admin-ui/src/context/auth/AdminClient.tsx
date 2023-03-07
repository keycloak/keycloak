import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import Keycloak from "keycloak-js";
import { DependencyList, useEffect } from "react";
import { useErrorHandler } from "react-error-boundary";

import environment from "../../environment";
import { createNamedContext, useRequiredContext } from "ui-shared";

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
  const onError = useErrorHandler();
  const controller = new AbortController();
  const { signal } = controller;

  useEffect(() => {
    adminClientCall()
      .then((result) => {
        if (!signal.aborted) {
          callback(result);
        }
      })
      .catch((error) => {
        if (!signal.aborted) {
          onError(error);
        }
      });

    return () => controller.abort();
  }, deps);
}

export async function initAdminClient() {
  const keycloak = new Keycloak({
    url: environment.authServerUrl,
    realm: environment.loginRealm,
    clientId: environment.isRunningAsTheme
      ? "security-admin-console"
      : "security-admin-console-v2",
  });

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
