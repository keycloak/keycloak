import { environment } from "./environment";
import { keycloak } from "./keycloak";
import { UserRepresentation } from "./representations";
import { joinPath } from "./utils/joinPath";

export const fetchPersonalInfo = (params: RequestInit) =>
  get<UserRepresentation>("/", params);

async function get<T>(path: string, params: RequestInit): Promise<T> {
  const url = joinPath(
    environment.authServerUrl,
    "realms",
    environment.loginRealm,
    "account",
    path
  );

  const response = await fetch(url, {
    ...params,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${await getAccessToken()}`,
    },
  });

  return response.json();
}

async function getAccessToken() {
  try {
    await keycloak.updateToken(5);
  } catch (error) {
    keycloak.login();
  }

  return keycloak.token;
}
