import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import IdentityProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/identityProviderRepresentation";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";

import { DEFAULT_REALM, SERVER_URL } from "./constants";

const adminClient = new KeycloakAdminClient({
  baseUrl: SERVER_URL,
  realmName: DEFAULT_REALM,
});

await adminClient.auth({
  username: "admin",
  password: "admin",
  grantType: "password",
  clientId: "admin-cli",
});

export async function useTheme() {
  const masterRealm = await adminClient.realms.findOne({
    realm: DEFAULT_REALM,
  });

  await adminClient.realms.update(
    { realm: DEFAULT_REALM },
    { ...masterRealm, accountTheme: "keycloak.v3" },
  );
}

export async function importRealm(realm: RealmRepresentation) {
  await adminClient.realms.create(realm);
}

export async function deleteRealm(realm: string) {
  await adminClient.realms.del({ realm });
}

export async function createClient(
  client: ClientRepresentation,
): Promise<string> {
  return adminClient.clients.create(client).then((client) => client.id);
}

export async function findClientByClientId(clientId: string) {
  return adminClient.clients
    .find({ clientId })
    .then((clientArray) => clientArray[0]?.["id"]);
}

export async function deleteClient(id: string) {
  await adminClient.clients.del({ id });
}

export async function createIdentityProvider(
  idp: IdentityProviderRepresentation,
  realm = DEFAULT_REALM,
): Promise<string> {
  return adminClient.identityProviders.create({ ...idp, realm })["id"];
}

export async function deleteIdentityProvider(
  alias: string,
  realm = DEFAULT_REALM,
) {
  await adminClient.identityProviders.del({ alias, realm });
}

export async function importUserProfile(
  userProfile: UserProfileConfig,
  realm: string,
) {
  await adminClient.users.updateProfile({ ...userProfile, realm });
}

export async function enableLocalization(realm = DEFAULT_REALM) {
  const realmRepresentation = await adminClient.realms.findOne({ realm });
  await adminClient.realms.update(
    { realm },
    {
      ...realmRepresentation,
      internationalizationEnabled: true,
      supportedLocales: ["en", "nl", "de"],
    },
  );
}

export async function createUser(
  user: UserRepresentation,
  realm = DEFAULT_REALM,
) {
  try {
    await adminClient.users.create({ ...user, realm });
  } catch (error) {
    console.error(error);
  }
}

export async function createRandomUserWithPassword(
  username: string,
  password: string,
  realm: string,
  props?: UserRepresentation,
): Promise<string> {
  await adminClient.auth({
    username: "admin",
    password: "admin",
    grantType: "password",
    clientId: "admin-cli",
  });
  return createUser(
    {
      username: username,
      enabled: true,
      credentials: [
        {
          type: "password",
          value: password,
        },
      ],
      ...props,
    },
    realm,
  ).then(() => username);
}

export async function getUserByUsername(username: string, realm: string) {
  const users = await adminClient.users.find({ username, realm, exact: true });
  return users.length > 0 ? users[0] : undefined;
}

export async function deleteUser(username: string, realm = DEFAULT_REALM) {
  try {
    const users = await adminClient.users.find({ username, realm });
    if (users.length === 0) {
      console.warn(`User ${username} not found in realm ${realm}`);
      return;
    }
    const { id } = users[0];
    await adminClient.users.del({ id: id!, realm });
  } catch (error) {
    console.error(error);
  }
}

export async function updateUser(user: UserRepresentation, realm: string) {
  try {
    await adminClient.users.update({ id: user.id!, realm }, user);
  } catch (error) {
    console.error(error);
  }
}

export async function getCredentials(id: string, realm: string) {
  try {
    return await adminClient.users.getCredentials({ id, realm });
  } catch (error) {
    console.error(error);
  }
}

export async function deleteCredential(
  id: string,
  credentialId: string,
  realm: string,
) {
  try {
    await adminClient.users.deleteCredential({ id, credentialId, realm });
  } catch (error) {
    console.error(error);
  }
}
