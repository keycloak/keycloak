import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";

const adminClient = new KeycloakAdminClient({
  baseUrl: process.env.KEYCLOAK_SERVER || "http://127.0.0.1:8180",
  realmName: "master",
});

await adminClient.auth({
  username: "admin",
  password: "admin",
  grantType: "password",
  clientId: "admin-cli",
});

export async function useTheme() {
  const masterRealm = await adminClient.realms.findOne({ realm: "master" });
  await adminClient.realms.update(
    { realm: "master" },
    { ...masterRealm, accountTheme: "keycloak.v3" },
  );
}

export async function importRealm(realm: RealmRepresentation) {
  await adminClient.realms.create(realm);
}

export async function deleteRealm(realm: string) {
  await adminClient.realms.del({ realm });
}

export async function importUserProfile(
  userProfile: UserProfileConfig,
  realm: string,
) {
  await adminClient.users.updateProfile({ ...userProfile, realm });
}

export async function enableLocalization(realm: string) {
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

export async function createUser(user: UserRepresentation, realm: string) {
  try {
    await adminClient.users.create({ ...user, realm });
  } catch (error) {
    console.error(error);
  }
}

export async function deleteUser(username: string, realm: string) {
  try {
    const users = await adminClient.users.find({ username, realm });
    const { id } = users[0];
    await adminClient.users.del({ id: id!, realm });
  } catch (error) {
    console.error(error);
  }
}
