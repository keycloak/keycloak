import {
  KeycloakContext,
  type BaseEnvironment,
} from "@keycloak/keycloak-ui-shared";

import { CallOptions } from "./api/methods";
import { Links, parseLinks } from "./api/parse-links";
import { parseResponse } from "./api/parse-response";
import {
  CredentialsIssuer,
  Permission,
  Resource,
  Scope,
  SupportedCredentialConfiguration,
} from "./api/representations";
import { request } from "./api/request";
import { joinPath } from "./utils/joinPath";

export const fetchResources = async (
  { signal, context }: CallOptions,
  requestParams: Record<string, string>,
  shared: boolean | undefined = false,
): Promise<{ data: Resource[]; links: Links }> => {
  const response = await request(
    `/resources${shared ? "/shared-with-me?" : "?"}`,
    context,
    { searchParams: shared ? requestParams : undefined, signal },
  );

  const links = parseLinks(response);

  return {
    data: checkResponse(await response.json()),
    links,
  };
};

export const fetchPermission = async (
  { signal, context }: CallOptions,
  resourceId: string,
): Promise<Permission[]> => {
  const response = await request(
    `/resources/${resourceId}/permissions`,
    context,
    { signal },
  );
  return parseResponse<Permission[]>(response);
};

export const updateRequest = (
  context: KeycloakContext<BaseEnvironment>,
  resourceId: string,
  username: string,
  scopes: Scope[] | string[],
) =>
  request(`/resources/${resourceId}/permissions`, context, {
    method: "PUT",
    body: [{ username, scopes }],
  });

export const updatePermissions = (
  context: KeycloakContext<BaseEnvironment>,
  resourceId: string,
  permissions: Permission[],
) =>
  request(`/resources/${resourceId}/permissions`, context, {
    method: "PUT",
    body: permissions,
  });

function checkResponse<T>(response: T) {
  if (!response) throw new Error("Could not fetch");
  return response;
}

export async function getIssuer(context: KeycloakContext<BaseEnvironment>) {
  const response = await request(
    joinPath(
      "/realms/",
      context.environment.realm,
      "/.well-known/openid-credential-issuer",
    ),
    context,
    {},
    new URL(
      joinPath(
        context.environment.serverBaseUrl,
        "/realms/",
        context.environment.realm,
        "/.well-known/openid-credential-issuer",
      ),
    ),
  );
  return parseResponse<CredentialsIssuer>(response);
}

export async function requestVCOffer(
  context: KeycloakContext<BaseEnvironment>,
  supportedCredentialConfiguration: SupportedCredentialConfiguration,
  credentialsIssuer: CredentialsIssuer,
) {
  const response = await request(
    "/protocol/oid4vc/credential-offer-uri",
    context,
    {
      searchParams: {
        credential_configuration_id: supportedCredentialConfiguration.id,
        type: "qr-code",
        width: "500",
        height: "500",
      },
    },
    new URL(
      joinPath(
        credentialsIssuer.credential_issuer +
          "/protocol/oid4vc/credential-offer-uri",
      ),
    ),
  );
  return response.blob();
}
