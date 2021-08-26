import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";

export default class AdminClient {
  private client: KeycloakAdminClient;
  constructor() {
    this.client = new KeycloakAdminClient({
      baseUrl: `${Cypress.env("KEYCLOAK_SERVER")}/auth`,
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

  async createRealm(realm: string) {
    await this.login();
    await this.client.realms.create({ realm });
  }

  async deleteRealm(realm: string) {
    await this.login();
    await this.client.realms.del({ realm });
  }

  async createClient(client: ClientRepresentation) {
    await this.login();
    await this.client.clients.create(client);
  }

  async deleteClient(clientName: string) {
    await this.login();
    const client = (
      await this.client.clients.find({ clientId: clientName })
    )[0];
    await this.client.clients.del({ id: client.id! });
  }

  async createSubGroups(groups: string[]) {
    await this.login();
    let parentGroup = undefined;
    const createdGroups = [];
    for (const group of groups) {
      if (!parentGroup) {
        parentGroup = await this.client.groups.create({ name: group });
      } else {
        parentGroup = await this.client.groups.setOrCreateChild(
          { id: parentGroup.id },
          { name: group }
        );
      }
      createdGroups.push(parentGroup);
    }
    return createdGroups;
  }

  async deleteGroups() {
    await this.login();
    const groups = await this.client.groups.find();
    for (const group of groups) {
      await this.client.groups.del({ id: group.id! });
    }
  }

  async createUser(user: UserRepresentation) {
    await this.login();
    return await this.client.users.create(user);
  }

  async createUserInGroup(username: string, groupId: string) {
    await this.login();
    const user = await this.createUser({ username, enabled: true });
    await this.client.users.addToGroup({ id: user.id!, groupId });
  }

  async deleteUser(username: string) {
    await this.login();
    const user = await this.client.users.find({ username });
    await this.client.users.del({ id: user[0].id! });
  }
}
