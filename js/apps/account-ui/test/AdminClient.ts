import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";

class AdminClient {
  private readonly client;
  constructor() {
    this.client = new KeycloakAdminClient({
      baseUrl: "http://127.0.0.1:8180",
      realmName: "master",
    });
  }

  async login() {
    await this.client.auth({
      username: "admin",
      password: "admin",
      grantType: "password",
      clientId: "admin-cli",
    });
  }

  async importRealm(realm: RealmRepresentation) {
    await this.client.realms.create(realm);
  }

  async deleteRealm(realm: string) {
    await this.client.realms.del({ realm });
  }
}

const adminClient = new AdminClient();

await adminClient.login();

export default adminClient;
