import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation";
import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { Credentials } from "@keycloak/keycloak-admin-client/lib/utils/auth";
import { merge } from "lodash-es";
import { SERVER_URL } from "../constants";

class AdminClient {
  readonly #client = new KeycloakAdminClient({
    baseUrl: SERVER_URL,
    realmName: "master",
  });

  #login() {
    return this.inRealm("master", () =>
      this.#client.auth({
        username: "admin",
        password: "admin",
        grantType: "password",
        clientId: "admin-cli",
      }),
    );
  }

  async auth(credentials: Credentials) {
    return this.#client.auth(credentials);
  }

  async loginUser(username: string, password: string, clientId: string) {
    return this.#client.auth({
      username: username,
      password: password,
      grantType: "password",
      clientId: clientId,
    });
  }

  async createRealm(realm: string, payload?: RealmRepresentation) {
    await this.#login();
    await this.#client.realms.create({ realm, ...payload });
  }

  async updateRealm(realm: string, payload: RealmRepresentation) {
    await this.#login();
    await this.#client.realms.update({ realm }, payload);
  }

  async getRealm(realm: string) {
    await this.#login();
    return await this.#client.realms.findOne({ realm });
  }

  async deleteRealm(realm: string) {
    await this.#login();
    await this.#client.realms.del({ realm });
  }

  async createClient(
    client: ClientRepresentation & {
      realm?: string;
    },
  ) {
    await this.#login();
    await this.#client.clients.create(client);
  }

  async deleteClient(clientName: string) {
    await this.#login();
    const client = (
      await this.#client.clients.find({ clientId: clientName })
    )[0];

    if (client) {
      await this.#client.clients.del({ id: client.id! });
    }
  }

  async getClient(clientName: string) {
    await this.#login();
    return (await this.#client.clients.find({ clientId: clientName }))[0];
  }

  async createGroup(groupName: string) {
    await this.#login();
    return await this.#client.groups.create({ name: groupName });
  }

  async createSubGroups(groups: string[]) {
    await this.#login();
    let parentGroup = undefined;
    const createdGroups = [];
    for (const group of groups) {
      if (!parentGroup) {
        parentGroup = await this.#client.groups.create({ name: group });
      } else {
        parentGroup = await this.#client.groups.createChildGroup(
          { id: parentGroup.id },
          { name: group },
        );
      }
      createdGroups.push(parentGroup);
    }
    return createdGroups;
  }

  async deleteGroups() {
    await this.#login();
    const groups = await this.#client.groups.find();
    for (const group of groups) {
      await this.#client.groups.del({ id: group.id! });
    }
  }

  async createUser(user: UserRepresentation) {
    await this.#login();

    const { id } = await this.#client.users.create(user);
    const createdUser = await this.#client.users.findOne({ id });

    if (!createdUser) {
      throw new Error(
        "Unable to create user, created user could not be found.",
      );
    }

    return createdUser;
  }

  async updateUser(id: string, payload: UserRepresentation) {
    await this.#login();
    return this.#client.users.update({ id }, payload);
  }

  async getAdminUser() {
    await this.#login();
    const [user] = await this.#client.users.find({ username: "admin" });
    return user;
  }

  async addUserToGroup(userId: string, groupId: string) {
    await this.#login();
    await this.#client.users.addToGroup({ id: userId, groupId });
  }

  async createUserInGroup(username: string, groupId: string) {
    await this.#login();
    const user = await this.createUser({ username, enabled: true });
    await this.#client.users.addToGroup({ id: user.id!, groupId });
  }

  async addRealmRoleToUser(userId: string, roleName: string) {
    await this.#login();

    const realmRole = await this.#client.roles.findOneByName({
      name: roleName,
    });

    await this.#client.users.addRealmRoleMappings({
      id: userId,
      roles: [realmRole as RoleMappingPayload],
    });
  }

  async addClientRoleToUser(
    userId: string,
    clientId: string,
    roleNames: string[],
  ) {
    await this.#login();

    const client = await this.#client.clients.find({ clientId });
    const clientRoles = await Promise.all(
      roleNames.map(
        async (roleName) =>
          (await this.#client.clients.findRole({
            id: client[0].id!,
            roleName: roleName,
          })) as RoleMappingPayload,
      ),
    );
    await this.#client.users.addClientRoleMappings({
      id: userId,
      clientUniqueId: client[0].id!,
      roles: clientRoles,
    });
  }

  async deleteUser(username: string) {
    await this.#login();
    const user = await this.#client.users.find({ username });
    await this.#client.users.del({ id: user[0].id! });
  }

  async createClientScope(scope: ClientScopeRepresentation) {
    await this.#login();
    return await this.#client.clientScopes.create(scope);
  }

  async deleteClientScope(clientScopeName: string) {
    await this.#login();
    const clientScope = await this.#client.clientScopes.findOneByName({
      name: clientScopeName,
    });
    return await this.#client.clientScopes.del({ id: clientScope?.id! });
  }

  async existsClientScope(clientScopeName: string) {
    await this.#login();
    return (await this.#client.clientScopes.findOneByName({
      name: clientScopeName,
    })) == undefined
      ? false
      : true;
  }

  async addDefaultClientScopeInClient(
    clientScopeName: string,
    clientId: string,
  ) {
    await this.#login();
    const scope = await this.#client.clientScopes.findOneByName({
      name: clientScopeName,
    });
    const client = await this.#client.clients.find({ clientId: clientId });
    return await this.#client.clients.addDefaultClientScope({
      id: client[0]?.id!,
      clientScopeId: scope?.id!,
    });
  }

  async removeDefaultClientScopeInClient(
    clientScopeName: string,
    clientId: string,
  ) {
    await this.#login();
    const scope = await this.#client.clientScopes.findOneByName({
      name: clientScopeName,
    });
    const client = await this.#client.clients.find({ clientId: clientId });
    return await this.#client.clients.delDefaultClientScope({
      id: client[0]?.id!,
      clientScopeId: scope?.id!,
    });
  }

  async getUserProfile(realm: string) {
    await this.#login();

    return await this.#client.users.getProfile({ realm });
  }

  async updateUserProfile(realm: string, userProfile: UserProfileConfig) {
    await this.#login();

    await this.#client.users.updateProfile(merge(userProfile, { realm }));
  }

  async addGroupToProfile(realm: string, groupName: string) {
    await this.#login();

    const currentProfile = await this.#client.users.getProfile({ realm });

    await this.#client.users.updateProfile({
      ...currentProfile,
      realm,
      ...{ groups: [...currentProfile.groups!, { name: groupName }] },
    });
  }

  async createRealmRole(payload: RoleRepresentation) {
    await this.#login();

    return await this.#client.roles.create(payload);
  }

  async deleteRealmRole(name: string) {
    await this.#login();
    return await this.#client.roles.delByName({ name });
  }

  async createIdentityProvider(idpDisplayName: string, alias: string) {
    await this.#login();
    const identityProviders =
      (await this.#client.serverInfo.find()).identityProviders || [];
    const idp = identityProviders.find(({ name }) => name === idpDisplayName);
    await this.#client.identityProviders.create({
      providerId: idp?.id!,
      displayName: idpDisplayName,
      alias: alias,
    });
  }

  async deleteIdentityProvider(idpAlias: string) {
    await this.#login();
    await this.#client.identityProviders.del({
      alias: idpAlias,
    });
  }

  async unlinkAccountIdentityProvider(
    username: string,
    idpDisplayName: string,
  ) {
    await this.#login();
    const user = await this.#client.users.find({ username });
    const identityProviders =
      (await this.#client.serverInfo.find()).identityProviders || [];
    const idp = identityProviders.find(({ name }) => name === idpDisplayName);
    await this.#client.users.delFromFederatedIdentity({
      id: user[0].id!,
      federatedIdentityId: idp?.id!,
    });
  }

  async linkAccountIdentityProvider(username: string, idpDisplayName: string) {
    await this.#login();
    const user = await this.#client.users.find({ username });
    const identityProviders =
      (await this.#client.serverInfo.find()).identityProviders || [];
    const idp = identityProviders.find(({ name }) => name === idpDisplayName);
    const fedIdentity = {
      identityProvider: idp?.id,
      userId: "testUserIdApi",
      userName: "testUserNameApi",
    };
    await this.#client.users.addToFederatedIdentity({
      id: user[0].id!,
      federatedIdentityId: idp?.id!,
      federatedIdentity: fedIdentity,
    });
  }

  async addLocalizationText(locale: string, key: string, value: string) {
    await this.#login();
    await this.#client.realms.addLocalization(
      { realm: this.#client.realmName, selectedLocale: locale, key: key },
      value,
    );
  }

  async removeAllLocalizationTexts() {
    await this.#login();
    const localesWithTexts = await this.#client.realms.getRealmSpecificLocales({
      realm: this.#client.realmName,
    });
    await Promise.all(
      localesWithTexts.map((locale) =>
        this.#client.realms.deleteRealmLocalizationTexts({
          realm: this.#client.realmName,
          selectedLocale: locale,
        }),
      ),
    );
  }

  async inRealm<T>(realm: string, fn: () => Promise<T>) {
    const prevRealm = this.#client.realmName;
    this.#client.realmName = realm;
    try {
      return await fn();
    } finally {
      this.#client.realmName = prevRealm;
    }
  }

  async createOrganization(org: OrganizationRepresentation) {
    await this.#login();
    await this.#client.organizations.create(org);
  }

  async deleteOrganization(name: string) {
    await this.#login();
    const { id } = (await this.#client.organizations.find({ search: name }))[0];
    await this.#client.organizations.delById({ id: id! });
  }
}

const adminClient = new AdminClient();

export default adminClient;
