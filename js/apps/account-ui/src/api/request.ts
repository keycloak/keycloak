import { environment } from "../environment";
import { keycloak } from "../keycloak";
import { joinPath } from "../utils/joinPath";
import { CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON } from "./constants";

export type RequestOptions = {
  signal?: AbortSignal;
  method?: "POST" | "PUT" | "DELETE";
  searchParams?: Record<string, string>;
  body?: unknown;
};

export async function request(
  path: string,
  { signal, method, searchParams, body }: RequestOptions = {}
): Promise<Response> {
  const url = new URL(
    joinPath(
      environment.authServerUrl,
      "realms",
      environment.loginRealm,
      "account",
      path
    )
  );

  if (searchParams) {
    Object.entries(searchParams).forEach(([key, value]) =>
      url.searchParams.set(key, value)
    );
  }

  return fetch(url, {
    signal,
    method,
    body: body ? JSON.stringify(body) : undefined,
    headers: {
      [CONTENT_TYPE_HEADER]: CONTENT_TYPE_JSON,
      authorization: `Bearer ${await getAccessToken()}`,
    },
  });
}

async function getAccessToken() {
  try {
    await keycloak.updateToken(5);
  } catch (error) {
    await keycloak.login();
  }

  return keycloak.token;
}
