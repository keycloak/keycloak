import KeycloakAdminClient from "keycloak-admin";

export default class AdminClient {
  private client: KeycloakAdminClient;
  constructor() {
    this.client = new KeycloakAdminClient({
      baseUrl: "http://localhost:8180/auth",
      realmName: "master",
    });
  }

  private async login() {
    await this.client.auth({
      username: "admin",
      password: "admin",
      grantType: "password",
      clientId: "admin-cli",
    });
  }

  async deleteRealm(realm: string) {
    await this.login();
    await this.client.realms.del({ realm });
  }

  async deleteClient(clientName: string) {
    await this.login();
    const client = (
      await this.client.clients.find({ clientId: clientName })
    )[0];
    await this.client.clients.del({ id: client.id! });
  }
}
