import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type UserProfileConfig from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";

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
