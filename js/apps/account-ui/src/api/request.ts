import {
  KeycloakContext,
  type BaseEnvironment,
} from "@keycloak/keycloak-ui-shared";
import Keycloak from "keycloak-js";

import { joinPath } from "../utils/joinPath";
import { CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON } from "./constants";

export type RequestOptions = {
  signal?: AbortSignal;
  getAccessToken?: () => Promise<string | undefined>;
  method?: "POST" | "PUT" | "DELETE";
  searchParams?: Record<string, string>;
  body?: unknown;
};

async function _request(
  url: URL,
  keycloak: Keycloak,
  { signal, getAccessToken, method, searchParams, body }: RequestOptions = {},
): Promise<Response> {
  if (searchParams) {
    Object.entries(searchParams).forEach(([key, value]) =>
      url.searchParams.set(key, value),
    );
  }

  // Refresh first, then read keycloak.token at the last moment so a concurrent
  // refresh cannot make keycloak.fetch skip the Bearer→DPoP upgrade.
  await getAccessToken?.();

  return keycloak.fetch(url, {
    signal,
    method,
    body: body ? JSON.stringify(body) : undefined,
    headers: {
      [CONTENT_TYPE_HEADER]: CONTENT_TYPE_JSON,
      authorization: `Bearer ${keycloak.token}`,
    },
  });
}

export async function request(
  path: string,
  { environment, keycloak }: KeycloakContext<BaseEnvironment>,
  opts: RequestOptions = {},
  fullUrl?: URL,
) {
  if (typeof fullUrl === "undefined") {
    fullUrl = url(environment, path);
  }
  return _request(fullUrl, keycloak, {
    ...opts,
    getAccessToken: token(keycloak),
  });
}

export const url = (environment: BaseEnvironment, path: string) =>
  new URL(
    joinPath(
      environment.serverBaseUrl,
      "realms",
      environment.realm,
      "account",
      path,
    ),
  );

export const token = (keycloak: Keycloak) =>
  async function getAccessToken() {
    try {
      await keycloak.updateToken(5);
    } catch {
      await keycloak.login();
    }

    return keycloak.token;
  };
