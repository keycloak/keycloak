import type { ServerInfoRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/serverInfoRepresentation";

const SOCIAL_IDENTITY_PROVIDER_SPI =
  "org.keycloak.broker.social.SocialIdentityProvider";

export function isSocialIdentityProvider(
  providerId: string | undefined,
  serverInfo: ServerInfoRepresentation | undefined,
): boolean {
  if (!providerId) {
    return false;
  }

  return !!serverInfo?.componentTypes?.[SOCIAL_IDENTITY_PROVIDER_SPI]?.some(
    ({ id }) => id === providerId,
  );
}
