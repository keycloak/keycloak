import KeycloakAdminClient from "@keycloak/keycloak-admin-client";
import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation.js";
import type ClientScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientScopeRepresentation.js";
import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation.js";
import type OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation.js";
import type PolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation.js";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation.js";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation.js";
import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation.js";
import type { RoleMappingPayload } from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation.js";
import type { UserProfileConfig } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata.js";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation.js";
import type { Credentials } from "@keycloak/keycloak-admin-client/lib/utils/auth.js";

class AdminClient {
  readonly #client = new KeycloakAdminClient({
    baseUrl: "http://localhost:8080",
    realmName: "master",
  });

  #login() {
    return this.#client.auth({
      username: "admin",
      password: "admin",
      grantType: "password",
      clientId: "admin-cli",
    });
  }

  async auth(credentials: Credentials) {
    return this.#client.auth(credentials);
  }

  async createRealm(realm: string, payload?: RealmRepresentation) {
    await this.#login();
    return await this.#client.realms.create({ realm, ...payload });
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
    const foundRealm = await this.#client.realms.findOne({ realm });
    if (foundRealm) {
      await this.#client.realms.del({ realm });
    }
  }

  async createClient(
    client: ClientRepresentation & {
      realm?: string;
    },
  ) {
    await this.#login();
    return await this.#client.clients.create(client);
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

  async createGroup(groupName: string, realm: string = this.#client.realmName) {
    await this.#login();
    return await this.#client.groups.create({ name: groupName, realm });
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

  async createUser(user: UserRepresentation & { realm?: string }) {
    await this.#login();

    const { id } = await this.#client.users.create(user);
    const createdUser = await this.#client.users.findOne({
      id,
      realm: user.realm || this.#client.realmName,
    });

    if (!createdUser) {
      throw new Error(
        "Unable to create user, created user could not be found.",
      );
    }

    return createdUser;
  }

  async updateUser(
    id: string,
    payload: UserRepresentation & { realm: string },
  ) {
    await this.#login();
    const { realm, ...rest } = payload;
    const user = await this.#client.users.findOne({ id, realm });
    return this.#client.users.update(
      { id, realm: realm || this.#client.realmName },
      { ...user, ...rest },
    );
  }

  async addUserToGroup(userId: string, groupId: string) {
    await this.#login();
    await this.#client.users.addToGroup({ id: userId, groupId });
  }

  async addClientRoleToUser(
    userId: string,
    clientId: string,
    roleNames: string[],
    realmName: string = this.#client.realmName,
  ) {
    await this.#login();

    const client = await this.#client.clients.find({
      clientId,
      realm: realmName,
    });
    const clientRoles = await Promise.all(
      roleNames.map(
        async (roleName) =>
          (await this.#client.clients.findRole({
            id: client[0].id!,
            roleName: roleName,
            realm: realmName,
          })) as RoleMappingPayload,
      ),
    );
    await this.#client.users.addClientRoleMappings({
      id: userId,
      clientUniqueId: client[0].id!,
      roles: clientRoles,
      realm: realmName,
    });
  }

  async addRealmRoleToGroup(
    groupId: string,
    roleName: string,
    realmName: string = this.#client.realmName,
  ) {
    await this.#login();

    const realmRole = await this.#client.roles.findOneByName({
      name: roleName,
      realm: realmName,
    });

    await this.#client.groups.addRealmRoleMappings({
      id: groupId,
      roles: [realmRole as RoleMappingPayload],
      realm: realmName,
    });
  }

  async deleteUser(
    username: string,
    realm: string = this.#client.realmName,
    ignoreNonExisting: boolean = false,
  ) {
    await this.#login();
    const foundUsers = await this.#client.users.find({ username, realm });
    if (foundUsers.length == 0) {
      if (ignoreNonExisting) {
        return;
      } else {
        throw new Error(`User not found: ${username}`);
      }
    }

    await this.#client.users.del({ id: foundUsers[0].id!, realm });
  }

  async createClientScope(
    scope: ClientScopeRepresentation & { realm?: string },
  ) {
    await this.#login();
    return await this.#client.clientScopes.create(scope);
  }

  async addDefaultClientScopeInClient(
    clientScopeName: string,
    clientId: string,
    realm: string = this.#client.realmName,
  ) {
    await this.#login();
    const scope = await this.#client.clientScopes.findOneByName({
      realm,
      name: clientScopeName,
    });
    const client = await this.#client.clients.find({
      clientId: clientId,
      realm,
    });
    return await this.#client.clients.addDefaultClientScope({
      realm,
      id: client[0]?.id!,
      clientScopeId: scope?.id!,
    });
  }

  async addUserProfile(realm: string, userProfile: UserProfileConfig) {
    await this.#login();
    const currentProfile = await this.#client.users.getProfile({ realm });
    await this.#client.users.updateProfile({
      groups: [...(userProfile.groups || []), ...(currentProfile.groups || [])],
      attributes: [
        ...(userProfile.attributes || []),
        ...(currentProfile.attributes || []),
      ],
      realm,
    });
  }

  async createRealmRole(payload: RoleRepresentation & { realm?: string }) {
    await this.#login();

    return await this.#client.roles.create(payload);
  }

  async createClientRole(
    id: string,
    payload: RoleRepresentation & { realm?: string },
  ) {
    await this.#login();

    return await this.#client.clients.createRole({
      id,
      ...payload,
    });
  }

  async createClientPolicy(
    name: string,
    description: string,
    realm: string = this.#client.realmName,
  ) {
    await this.#login();
    const { policies } = await this.#client.clientPolicies.listPolicies({
      realm,
    });
    return await this.#client.clientPolicies.updatePolicy({
      realm,
      policies: [...policies!, { name, description }],
    });
  }

  async deleteClientPolicy(
    name: string,
    realm: string = this.#client.realmName,
  ) {
    await this.#login();
    const { policies } = await this.#client.clientPolicies.listPolicies({
      realm,
    });
    return await this.#client.clientPolicies.updatePolicy({
      realm,
      policies: [...policies!.filter((policy) => policy.name !== name)],
    });
  }

  async createClientProfile(
    name: string,
    description: string,
    realm: string = this.#client.realmName,
  ) {
    await this.#login();
    const { profiles } = await this.#client.clientPolicies.listProfiles({
      realm,
    });
    return await this.#client.clientPolicies.createProfiles({
      realm,
      profiles: [...profiles!, { name, description }],
    });
  }

  async deleteRealmRole(name: string, realm: string = this.#client.realmName) {
    await this.#login();
    return await this.#client.roles.delByName({ name, realm });
  }

  async createIdentityProvider(
    idpDisplayName: string,
    alias: string,
    realm: string = this.#client.realmName,
  ) {
    await this.#login();
    const identityProviders =
      (await this.#client.serverInfo.find({ realm })).identityProviders || [];
    const idp = identityProviders.find(({ name }) => name === idpDisplayName);
    await this.#client.identityProviders.create({
      realm,
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

  async addLocalizationText(
    locale: string,
    key: string,
    value: string,
    realm: string = this.#client.realmName,
  ) {
    await this.#login();
    await this.#client.realms.addLocalization(
      { realm, selectedLocale: locale, key: key },
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

  async createOrganization(
    org: OrganizationRepresentation & { realm?: string },
  ) {
    await this.#login();
    await this.#client.organizations.create(org);
  }

  async deleteOrganization(
    name: string,
    realm: string = this.#client.realmName,
  ) {
    await this.#login();
    const found = await this.#client.organizations.find({
      search: name,
      realm,
    });
    if (found.length !== 0) {
      await this.#client.organizations.delById({ id: found[0].id!, realm });
    }
  }

  async copyFlow(
    name: string,
    newName: string,
    realmName: string = this.#client.realmName,
  ) {
    await this.#login();
    await this.#client.authenticationManagement.copyFlow({
      flow: name,
      newName: newName,
      realm: realmName,
    });
  }

  async deleteAllTokens(realm: string = this.#client.realmName) {
    await this.#login();
    const tokens = await this.#client.realms.getClientsInitialAccess({ realm });
    for (const token of tokens) {
      await this.#client.realms.delClientsInitialAccess({
        realm: realm,
        id: token.id!,
      });
    }
  }

  async addKeyProvider(
    name: string,
    active: boolean,
    enabled: boolean,
    providerType: string,
    realm: string = this.#client.realmName,
  ) {
    await this.#login();
    await this.#client.components.create({
      realm,
      name,
      config: {
        enabled: [`${enabled}`],
        active: [`${active}`],
        priority: ["0"],
      },
      providerId: providerType,
      providerType: "org.keycloak.keys.KeyProvider",
    });
  }

  async createUserFederation(
    realmName: string,
    federatedIdentity: ComponentRepresentation,
  ) {
    await this.#login();
    const realm = await this.#client.realms.findOne({
      realm: realmName,
    });
    if (!realm) {
      throw new Error(`Realm ${realmName} not found`);
    }

    await this.#client.components.create({
      realm: realmName,
      parentId: realm.id,
      providerType: "org.keycloak.storage.UserStorageProvider",
      ...federatedIdentity,
    });
  }

  async #getPermissionClient(realm: string = this.#client.realmName) {
    const clients = await this.#client.clients.find({
      realm,
      clientId: "admin-permissions",
    });
    if (clients.length === 0)
      throw new Error("Client admin-permissions not found");
    return clients[0];
  }

  async createPermission({
    realm,
    ...permission
  }: PolicyRepresentation & { realm?: string }) {
    await this.#login();
    const client = await this.#getPermissionClient(realm);
    await this.#client.clients.createPermission(
      { id: client.id!, type: "scope", realm },
      permission,
    );
  }

  async createUserPolicy({
    username,
    realm,
    ...policy
  }: PolicyRepresentation & { realm?: string; username: string }) {
    await this.#login();
    const user = await this.#client.users.find({ username, realm });
    if (user.length === 0) {
      throw new Error(`User ${username} not found`);
    }
    const client = await this.#getPermissionClient(realm);
    return this.#client.clients.createPolicy(
      { id: client.id!, type: policy.type!, realm },
      {
        users: [user[0].id!],
        ...policy,
      },
    );
  }

  async createResource(
    clientId: string,
    resource: ResourceRepresentation & { realm?: string },
  ) {
    await this.#login();
    const { realm = this.#client.realmName, ...payload } = resource;

    const client = (await this.#client.clients.find({ clientId, realm }))[0];

    if (!client?.id) {
      throw new Error(`Client ${clientId} not found in realm ${realm}`);
    }

    return await this.#client.clients.createResource(
      { id: client.id, realm },
      payload,
    );
  }

  async findUserByUsername(
    realm: string,
    username: string,
  ): Promise<UserRepresentation> {
    await this.#login();

    const users = await this.#client.users.find({
      realm,
      username,
      exact: true,
      max: 1,
    });

    return users[0];
  }
}

const adminClient = new AdminClient();

export default adminClient;
