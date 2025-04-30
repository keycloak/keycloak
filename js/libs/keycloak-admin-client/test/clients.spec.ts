// tslint:disable:no-unused-expression
import { faker } from "@faker-js/faker";
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import type ClientRepresentation from "../src/defs/clientRepresentation.js";
import type ClientScopeRepresentation from "../src/defs/clientScopeRepresentation.js";
import type PolicyRepresentation from "../src/defs/policyRepresentation.js";
import { Logic } from "../src/defs/policyRepresentation.js";
import type ProtocolMapperRepresentation from "../src/defs/protocolMapperRepresentation.js";
import type ResourceRepresentation from "../src/defs/resourceRepresentation.js";
import type ScopeRepresentation from "../src/defs/scopeRepresentation.js";
import type UserRepresentation from "../src/defs/userRepresentation.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Clients", () => {
  let kcAdminClient: KeycloakAdminClient;
  let currentClient: ClientRepresentation;
  let currentClientScope: ClientScopeRepresentation;
  let currentRoleName: string;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);

    // create client and also test it
    // NOTICE: to be clear, clientId stands for the property `clientId` of client
    // clientUniqueId stands for property `id` of client
    const clientId = faker.internet.username();
    const createdClient = await kcAdminClient.clients.create({
      clientId,
    });
    expect(createdClient.id).to.be.ok;

    const client = await kcAdminClient.clients.findOne({
      id: createdClient.id,
    });
    expect(client).to.be.ok;
    currentClient = client!;
  });

  after(async () => {
    // delete the current one
    await kcAdminClient.clients.del({
      id: currentClient.id!,
    });
  });

  it("list clients", async () => {
    const clients = await kcAdminClient.clients.find();
    expect(clients).to.be.ok;
  });

  it("get single client", async () => {
    const clientUniqueId = currentClient.id!;
    const client = await kcAdminClient.clients.findOne({
      id: clientUniqueId,
    });
    // not sure why entity from list api will not have property: authorizationServicesEnabled
    expect(client).to.deep.include(currentClient);
  });

  it("update single client", async () => {
    const { clientId, id: clientUniqueId } = currentClient;
    await kcAdminClient.clients.update(
      { id: clientUniqueId! },
      {
        // clientId is required in client update. no idea why...
        clientId,
        description: "test",
      },
    );

    const client = await kcAdminClient.clients.findOne({
      id: clientUniqueId!,
    });
    expect(client).to.include({
      description: "test",
    });
  });

  it("delete single client", async () => {
    // create another one for delete test
    const clientId = faker.internet.username();
    const { id } = await kcAdminClient.clients.create({
      clientId,
    });

    // delete it
    await kcAdminClient.clients.del({
      id,
    });

    const delClient = await kcAdminClient.clients.findOne({
      id,
    });
    expect(delClient).to.be.null;
  });

  /**
   * client roles
   */
  describe("client roles", () => {
    before(async () => {
      const roleName = faker.internet.username();
      // create a client role
      const { roleName: createdRoleName } =
        await kcAdminClient.clients.createRole({
          id: currentClient.id,
          name: roleName,
        });

      expect(createdRoleName).to.be.equal(roleName);

      // assign currentClientRole
      currentRoleName = roleName;
    });

    after(async () => {
      // delete client role
      await kcAdminClient.clients.delRole({
        id: currentClient.id!,
        roleName: currentRoleName,
      });
    });

    it("list the client roles", async () => {
      const roles = await kcAdminClient.clients.listRoles({
        id: currentClient.id!,
      });

      expect(roles[0]).to.include({
        name: currentRoleName,
      });
    });

    it("find the client role", async () => {
      const role = await kcAdminClient.clients.findRole({
        id: currentClient.id!,
        roleName: currentRoleName,
      });

      expect(role).to.include({
        name: currentRoleName,
        clientRole: true,
        containerId: currentClient.id,
      });
    });

    it("update the client role", async () => {
      // NOTICE: roleName MUST be in the payload, no idea why...
      const delta = {
        name: currentRoleName,
        description: "test",
      };
      await kcAdminClient.clients.updateRole(
        {
          id: currentClient.id!,
          roleName: currentRoleName,
        },
        delta,
      );

      // check the change
      const role = await kcAdminClient.clients.findRole({
        id: currentClient.id!,
        roleName: currentRoleName,
      });

      expect(role).to.include(delta);
    });

    it("delete a client role", async () => {
      const roleName = faker.internet.username();
      // create a client role
      await kcAdminClient.clients.createRole({
        id: currentClient.id,
        name: roleName,
      });

      // delete
      await kcAdminClient.clients.delRole({
        id: currentClient.id!,
        roleName,
      });

      // check it's null
      const role = await kcAdminClient.clients.findRole({
        id: currentClient.id!,
        roleName,
      });

      expect(role).to.be.null;
    });
  });

  describe("client secret", () => {
    before(async () => {
      const { clientId, id: clientUniqueId } = currentClient;
      // update with serviceAccountsEnabled: true
      await kcAdminClient.clients.update(
        {
          id: clientUniqueId!,
        },
        {
          clientId,
          serviceAccountsEnabled: true,
        },
      );
    });

    it("get client secret", async () => {
      const credential = await kcAdminClient.clients.getClientSecret({
        id: currentClient.id!,
      });

      expect(credential).to.have.all.keys("type", "value");
    });

    it("generate new client secret", async () => {
      const newCredential = await kcAdminClient.clients.generateNewClientSecret(
        {
          id: currentClient.id!,
        },
      );

      const credential = await kcAdminClient.clients.getClientSecret({
        id: currentClient.id!,
      });

      expect(newCredential).to.be.eql(credential);
    });

    it("generate new registration access token", async () => {
      const newRegistrationAccessToken =
        await kcAdminClient.clients.generateRegistrationAccessToken({
          id: currentClient.id!,
        });

      expect(newRegistrationAccessToken).to.be.ok;
    });

    it("invalidate rotation token", async () => {
      await kcAdminClient.clients.invalidateSecret({
        id: currentClient.id!,
      });
    });

    it("get installation providers", async () => {
      const installationProvider =
        await kcAdminClient.clients.getInstallationProviders({
          id: currentClient.id!,
          providerId: "keycloak-oidc-jboss-subsystem",
        });
      expect(installationProvider).to.be.ok;
      expect(typeof installationProvider).to.be.equal("string");
    });

    it("get service account user", async () => {
      const serviceAccountUser =
        await kcAdminClient.clients.getServiceAccountUser({
          id: currentClient.id!,
        });

      expect(serviceAccountUser).to.be.ok;
    });
  });

  describe("default client scopes", () => {
    let dummyClientScope: ClientScopeRepresentation;

    beforeEach(async () => {
      dummyClientScope = {
        name: "does-anyone-read-this",
        description: "Oh - seems like you are reading  Hey there!",
        protocol: "openid-connect",
      };

      // setup dummy client scope
      await kcAdminClient.clientScopes.create(dummyClientScope);
      currentClientScope = (await kcAdminClient.clientScopes.findOneByName({
        name: dummyClientScope.name!,
      }))!;
    });

    afterEach(async () => {
      // cleanup default scopes
      try {
        const { id } = currentClient;
        const { id: clientScopeId } = currentClientScope;
        await kcAdminClient.clients.delDefaultClientScope({
          clientScopeId: clientScopeId!,
          id: id!,
        });
      } catch {
        // ignore
      }

      // cleanup client scopes
      try {
        await kcAdminClient.clientScopes.delByName({
          name: dummyClientScope.name!,
        });
      } catch {
        // ignore
      }
    });

    it("list default client scopes", async () => {
      const defaultClientScopes =
        await kcAdminClient.clients.listDefaultClientScopes({
          id: currentClient.id!,
        });

      expect(defaultClientScopes).to.be.ok;
    });

    it("add default client scope", async () => {
      const { id } = currentClient;
      const { id: clientScopeId } = currentClientScope;

      await kcAdminClient.clients.addDefaultClientScope({
        id: id!,
        clientScopeId: clientScopeId!,
      });

      const defaultScopes = await kcAdminClient.clients.listDefaultClientScopes(
        { id: id! },
      );

      expect(defaultScopes).to.be.ok;

      const clientScope = defaultScopes.find(
        (scope) => scope.id === clientScopeId,
      );
      expect(clientScope).to.be.ok;
    });

    it("delete default client scope", async () => {
      const { id } = currentClient;
      const { id: clientScopeId } = currentClientScope;

      await kcAdminClient.clients.addDefaultClientScope({
        id: id!,
        clientScopeId: clientScopeId!,
      });

      await kcAdminClient.clients.delDefaultClientScope({
        id: id!,
        clientScopeId: clientScopeId!,
      });
      const defaultScopes = await kcAdminClient.clients.listDefaultClientScopes(
        { id: id! },
      );

      const clientScope = defaultScopes.find(
        (scope) => scope.id === clientScopeId,
      );
      expect(clientScope).not.to.be.ok;
    });
  });

  describe("optional client scopes", () => {
    let dummyClientScope: ClientScopeRepresentation;

    beforeEach(async () => {
      dummyClientScope = {
        name: "i-hope-your-well",
        description: "Everyone has that one friend.",
        protocol: "openid-connect",
      };

      // setup dummy client scope
      await kcAdminClient.clientScopes.create(dummyClientScope);
      currentClientScope = (await kcAdminClient.clientScopes.findOneByName({
        name: dummyClientScope.name!,
      }))!;
    });

    afterEach(async () => {
      // cleanup optional scopes
      try {
        const { id } = currentClient;
        const { id: clientScopeId } = currentClientScope;
        await kcAdminClient.clients.delOptionalClientScope({
          clientScopeId: clientScopeId!,
          id: id!,
        });
      } catch {
        // ignore
      }

      // cleanup client scopes
      try {
        await kcAdminClient.clientScopes.delByName({
          name: dummyClientScope.name!,
        });
      } catch {
        // ignore
      }
    });

    it("list optional client scopes", async () => {
      const optionalClientScopes =
        await kcAdminClient.clients.listOptionalClientScopes({
          id: currentClient.id!,
        });

      expect(optionalClientScopes).to.be.ok;
    });

    it("add optional client scope", async () => {
      const { id } = currentClient;
      const { id: clientScopeId } = currentClientScope;

      await kcAdminClient.clients.addOptionalClientScope({
        id: id!,
        clientScopeId: clientScopeId!,
      });

      const optionalScopes =
        await kcAdminClient.clients.listOptionalClientScopes({ id: id! });

      expect(optionalScopes).to.be.ok;

      const clientScope = optionalScopes.find(
        (scope) => scope.id === clientScopeId,
      );
      expect(clientScope).to.be.ok;
    });

    it("delete optional client scope", async () => {
      const { id } = currentClient;
      const { id: clientScopeId } = currentClientScope;

      await kcAdminClient.clients.addOptionalClientScope({
        id: id!,
        clientScopeId: clientScopeId!,
      });

      await kcAdminClient.clients.delOptionalClientScope({
        id: id!,
        clientScopeId: clientScopeId!,
      });
      const optionalScopes =
        await kcAdminClient.clients.listOptionalClientScopes({ id: id! });

      const clientScope = optionalScopes.find(
        (scope) => scope.id === clientScopeId,
      );
      expect(clientScope).not.to.be.ok;
    });
  });

  describe("protocol mappers", () => {
    let dummyMapper: ProtocolMapperRepresentation;

    beforeEach(() => {
      dummyMapper = {
        name: "become-a-farmer",
        protocol: "openid-connect",
        protocolMapper: "oidc-role-name-mapper",
        config: {
          role: "admin",
          "new.role.name": "farmer",
        },
      };
    });

    afterEach(async () => {
      try {
        const { id: clientUniqueId } = currentClient;
        const { id: mapperId } =
          (await kcAdminClient.clients.findProtocolMapperByName({
            id: clientUniqueId!,
            name: dummyMapper.name!,
          }))!;
        await kcAdminClient.clients.delProtocolMapper({
          id: clientUniqueId!,
          mapperId: mapperId!,
        });
      } catch {
        // ignore
      }
    });

    it("list protocol mappers", async () => {
      const { id } = currentClient;
      const mapperList = await kcAdminClient.clients.listProtocolMappers({
        id: id!,
      });
      expect(mapperList).to.be.ok;
    });

    it("add multiple protocol mappers", async () => {
      const { id } = currentClient;
      await kcAdminClient.clients.addMultipleProtocolMappers({ id: id! }, [
        dummyMapper,
      ]);

      const mapper = (await kcAdminClient.clients.findProtocolMapperByName({
        id: id!,
        name: dummyMapper.name!,
      }))!;
      expect(mapper).to.be.ok;
      expect(mapper.protocol).to.eq(dummyMapper.protocol);
      expect(mapper.protocolMapper).to.eq(dummyMapper.protocolMapper);
    });

    it("add single protocol mapper", async () => {
      const { id } = currentClient;
      await kcAdminClient.clients.addProtocolMapper({ id: id! }, dummyMapper);

      const mapper = (await kcAdminClient.clients.findProtocolMapperByName({
        id: id!,
        name: dummyMapper.name!,
      }))!;
      expect(mapper).to.be.ok;
      expect(mapper.protocol).to.eq(dummyMapper.protocol);
      expect(mapper.protocolMapper).to.eq(dummyMapper.protocolMapper);
    });

    it("find protocol mapper by id", async () => {
      const { id } = currentClient;
      await kcAdminClient.clients.addProtocolMapper({ id: id! }, dummyMapper);

      const { id: mapperId } =
        (await kcAdminClient.clients.findProtocolMapperByName({
          id: id!,
          name: dummyMapper.name!,
        }))!;

      const mapper = await kcAdminClient.clients.findProtocolMapperById({
        mapperId: mapperId!,
        id: id!,
      });

      expect(mapper).to.be.ok;
      expect(mapper.id).to.eql(mapperId);
    });

    it("find protocol mapper by name", async () => {
      const { id } = currentClient;
      await kcAdminClient.clients.addProtocolMapper({ id: id! }, dummyMapper);

      const mapper = (await kcAdminClient.clients.findProtocolMapperByName({
        id: id!,
        name: dummyMapper.name!,
      }))!;

      expect(mapper).to.be.ok;
      expect(mapper.name).to.eql(dummyMapper.name);
    });

    it("find protocol mappers by protocol", async () => {
      const { id } = currentClient;
      await kcAdminClient.clients.addProtocolMapper({ id: id! }, dummyMapper);

      const mapperList =
        await kcAdminClient.clients.findProtocolMappersByProtocol({
          id: id!,
          protocol: dummyMapper.protocol!,
        });

      expect(mapperList).to.be.ok;
      expect(mapperList.length).to.be.gte(1);

      const mapper = mapperList.find((item) => item.name === dummyMapper.name);
      expect(mapper).to.be.ok;
    });

    it("update protocol mapper", async () => {
      const { id } = currentClient;

      dummyMapper.config = { "access.token.claim": "true" };
      await kcAdminClient.clients.addProtocolMapper({ id: id! }, dummyMapper);
      const mapper = (await kcAdminClient.clients.findProtocolMapperByName({
        id: id!,
        name: dummyMapper.name!,
      }))!;

      expect(mapper.config!["access.token.claim"]).to.eq("true");

      mapper.config = { "access.token.claim": "false" };

      await kcAdminClient.clients.updateProtocolMapper(
        { id: id!, mapperId: mapper.id! },
        mapper,
      );

      const updatedMapper =
        (await kcAdminClient.clients.findProtocolMapperByName({
          id: id!,
          name: dummyMapper.name!,
        }))!;

      expect(updatedMapper.config!["access.token.claim"]).to.eq("false");
    });

    it("delete protocol mapper", async () => {
      const { id } = currentClient;
      await kcAdminClient.clients.addProtocolMapper({ id: id! }, dummyMapper);

      const { id: mapperId } =
        (await kcAdminClient.clients.findProtocolMapperByName({
          id: id!,
          name: dummyMapper.name!,
        }))!;

      await kcAdminClient.clients.delProtocolMapper({
        id: id!,
        mapperId: mapperId!,
      });

      const mapper = await kcAdminClient.clients.findProtocolMapperByName({
        id: id!,
        name: dummyMapper.name!,
      });

      expect(mapper).not.to.be.ok;
    });
  });

  describe("scope mappings", () => {
    it("list client and realm scope mappings", async () => {
      const { id } = currentClient;
      const scopes = await kcAdminClient.clients.listScopeMappings({
        id: id!,
      });
      expect(scopes).to.be.ok;
    });

    describe("client", () => {
      const dummyRoleName = "clientScopeMappingsRole-dummy";

      beforeEach(async () => {
        const { id } = currentClient;
        await kcAdminClient.clients.createRole({
          id,
          name: dummyRoleName,
        });
      });

      afterEach(async () => {
        try {
          const { id } = currentClient;
          await kcAdminClient.clients.delRole({
            id: id!,
            roleName: dummyRoleName,
          });
        } catch {
          // ignore
        }
      });

      it("add scope mappings", async () => {
        const { id: clientUniqueId } = currentClient;

        const availableRoles =
          await kcAdminClient.clients.listAvailableClientScopeMappings({
            id: clientUniqueId!,
            client: clientUniqueId!,
          });

        await kcAdminClient.clients.addClientScopeMappings(
          {
            id: clientUniqueId!,
            client: clientUniqueId!,
          },
          availableRoles,
        );

        const roles = await kcAdminClient.clients.listClientScopeMappings({
          id: clientUniqueId!,
          client: clientUniqueId!,
        });

        expect(roles).to.be.ok;
        expect(roles).to.be.eql(availableRoles);
      });

      it("list scope mappings", async () => {
        const { id: clientUniqueId } = currentClient;
        const roles = await kcAdminClient.clients.listClientScopeMappings({
          id: clientUniqueId!,
          client: clientUniqueId!,
        });
        expect(roles).to.be.ok;
      });

      it("list available scope mappings", async () => {
        const { id: clientUniqueId } = currentClient;
        const roles =
          await kcAdminClient.clients.listAvailableClientScopeMappings({
            id: clientUniqueId!,
            client: clientUniqueId!,
          });
        expect(roles).to.be.ok;
      });

      it("list composite scope mappings", async () => {
        const { id: clientUniqueId } = currentClient;
        const roles =
          await kcAdminClient.clients.listCompositeClientScopeMappings({
            id: clientUniqueId!,
            client: clientUniqueId!,
          });
        expect(roles).to.be.ok;
      });

      it("delete scope mappings", async () => {
        const { id: clientUniqueId } = currentClient;

        const rolesBefore = await kcAdminClient.clients.listClientScopeMappings(
          {
            id: clientUniqueId!,
            client: clientUniqueId!,
          },
        );

        await kcAdminClient.clients.delClientScopeMappings(
          {
            id: clientUniqueId!,
            client: clientUniqueId!,
          },
          rolesBefore,
        );

        const rolesAfter = await kcAdminClient.clients.listClientScopeMappings({
          id: clientUniqueId!,
          client: clientUniqueId!,
        });

        expect(rolesAfter).to.be.ok;
        expect(rolesAfter).to.eql([]);
      });

      it("get effective scope mapping of all roles for a specific container", async () => {
        const { id: clientUniqueId } = currentClient;
        const roles = await kcAdminClient.clients.evaluatePermission({
          id: clientUniqueId!,
          roleContainer: "master",
          type: "granted",
          scope: "openid",
        });

        expect(roles).to.be.ok;
        expect(roles.length).to.be.eq(5);
      });

      it("get list of all protocol mappers", async () => {
        const { id: clientUniqueId } = currentClient;
        const protocolMappers =
          await kcAdminClient.clients.evaluateListProtocolMapper({
            id: clientUniqueId!,
            scope: "openid",
          });
        expect(protocolMappers).to.be.ok;
        expect(protocolMappers.length).to.be.gt(10);
      });

      it("get JSON with payload of examples", async () => {
        const { id: clientUniqueId } = currentClient;
        const username = faker.internet.username();
        const user = await kcAdminClient.users.create({
          username,
        });
        const accessToken =
          await kcAdminClient.clients.evaluateGenerateAccessToken({
            id: clientUniqueId!,
            userId: user.id,
            scope: "openid",
          });
        const idToken = await kcAdminClient.clients.evaluateGenerateIdToken({
          id: clientUniqueId!,
          userId: user.id,
          scope: "openid",
        });
        const userInfo = await kcAdminClient.clients.evaluateGenerateUserInfo({
          id: clientUniqueId!,
          userId: user.id,
          scope: "openid",
        });

        expect(accessToken).to.be.ok;
        expect(idToken).to.be.ok;
        expect(userInfo).to.be.ok;
        await kcAdminClient.users.del({ id: user.id });
      });
    });

    describe("realm", () => {
      const dummyRoleName = "realmScopeMappingsRole-dummy";

      beforeEach(async () => {
        await kcAdminClient.roles.create({
          name: dummyRoleName,
        });
      });

      afterEach(async () => {
        try {
          await kcAdminClient.roles.delByName({
            name: dummyRoleName,
          });
        } catch {
          // ignore
        }
      });

      it("add scope mappings", async () => {
        const { id } = currentClient;

        const availableRoles =
          await kcAdminClient.clients.listAvailableRealmScopeMappings({
            id: id!,
          });

        await kcAdminClient.clients.addRealmScopeMappings(
          { id: id! },
          availableRoles,
        );

        const roles = await kcAdminClient.clients.listRealmScopeMappings({
          id: id!,
        });

        expect(roles).to.be.ok;
        expect(roles).to.deep.members(availableRoles);
      });

      it("list scope mappings", async () => {
        const { id } = currentClient;
        const roles = await kcAdminClient.clients.listRealmScopeMappings({
          id: id!,
        });
        expect(roles).to.be.ok;
      });

      it("list available scope mappings", async () => {
        const { id } = currentClient;
        const roles =
          await kcAdminClient.clients.listAvailableRealmScopeMappings({
            id: id!,
          });
        expect(roles).to.be.ok;
      });

      it("list composite scope mappings", async () => {
        const { id } = currentClient;
        const roles =
          await kcAdminClient.clients.listCompositeRealmScopeMappings({
            id: id!,
          });
        expect(roles).to.be.ok;
      });

      it("delete scope mappings", async () => {
        const { id } = currentClient;

        const rolesBefore = await kcAdminClient.clients.listRealmScopeMappings({
          id: id!,
        });

        await kcAdminClient.clients.delRealmScopeMappings(
          { id: id! },
          rolesBefore,
        );

        const rolesAfter = await kcAdminClient.clients.listRealmScopeMappings({
          id: id!,
        });

        expect(rolesAfter).to.be.ok;
        expect(rolesAfter).to.eql([]);
      });
    });
  });

  describe("sessions", () => {
    it("list clients user sessions", async () => {
      const clientUniqueId = currentClient.id;
      const userSessions = await kcAdminClient.clients.listSessions({
        id: clientUniqueId!,
      });
      expect(userSessions).to.be.ok;
    });

    it("list clients offline user sessions", async () => {
      const clientUniqueId = currentClient.id;
      const userSessions = await kcAdminClient.clients.listOfflineSessions({
        id: clientUniqueId!,
      });
      expect(userSessions).to.be.ok;
    });

    it("list clients user session count", async () => {
      const clientUniqueId = currentClient.id;
      const userSessions = await kcAdminClient.clients.getSessionCount({
        id: clientUniqueId!,
      });
      expect(userSessions).to.be.ok;
    });

    it("list clients offline user session count", async () => {
      const clientUniqueId = currentClient.id;
      const userSessions = await kcAdminClient.clients.getOfflineSessionCount({
        id: clientUniqueId!,
      });
      expect(userSessions).to.be.ok;
    });
  });

  describe("nodes", () => {
    const host = "127.0.0.1";
    it("register a node manually", async () => {
      await kcAdminClient.clients.addClusterNode({
        id: currentClient.id!,
        node: host,
      });
      const client = (await kcAdminClient.clients.findOne({
        id: currentClient.id!,
      }))!;

      expect(Object.keys(client.registeredNodes!)).to.be.eql([host]);
    });

    it("remove registered host", async () => {
      await kcAdminClient.clients.deleteClusterNode({
        id: currentClient.id!,
        node: host,
      });
      const client = (await kcAdminClient.clients.findOne({
        id: currentClient.id!,
      }))!;

      expect(client.registeredNodes).to.be.undefined;
    });
  });

  describe("client attribute certificate", () => {
    const keystoreConfig = {
      format: "JKS",
      keyAlias: "new",
      keyPassword: "password",
      realmAlias: "master",
      realmCertificate: false,
      storePassword: "password",
    };
    const attr = "jwt.credential";

    it("generate and download keys", async () => {
      const result = await kcAdminClient.clients.generateAndDownloadKey(
        { id: currentClient.id!, attr },
        keystoreConfig,
      );

      expect(result).to.be.ok;
    });

    it("generate key and updated info", async () => {
      const certificate = await kcAdminClient.clients.generateKey({
        id: currentClient.id!,
        attr,
      });

      expect(certificate).to.be.ok;
      expect(certificate.certificate).to.be.ok;

      const info = await kcAdminClient.clients.getKeyInfo({
        id: currentClient.id!,
        attr,
      });
      expect(info).to.be.eql(certificate);
    });

    it("download key", async () => {
      const result = await kcAdminClient.clients.downloadKey(
        { id: currentClient.id!, attr },
        keystoreConfig,
      );

      expect(result).to.be.ok;
    });
  });

  describe("authorization", async () => {
    const resourceConfig = {
      name: "testResourceName",
      type: "testResourceType",
      scopeNames: ["testScopeA", "testScopeB", "testScopeC"],
    };
    const policyConfig = {
      name: "testPolicyName",
      type: "user",
      logic: Logic.POSITIVE,
    };
    const permissionConfig = {
      name: "testPermissionName",
      type: "scope",
      logic: Logic.POSITIVE,
    };
    let scopes: ScopeRepresentation[];
    let resource: ResourceRepresentation;
    let policy: PolicyRepresentation;
    let permission: PolicyRepresentation;
    let user: UserRepresentation;

    before("enable authorization services", async () => {
      await kcAdminClient.clients.update(
        { id: currentClient.id! },
        {
          clientId: currentClient.clientId,
          authorizationServicesEnabled: true,
          serviceAccountsEnabled: true,
        },
      );
    });

    before("create test user", async () => {
      const username = faker.internet.username();
      user = await kcAdminClient.users.create({
        username,
      });
    });

    after("delete test user", async () => {
      await kcAdminClient.users.del({
        id: user.id!,
      });
    });

    after("disable authorization services", async () => {
      await kcAdminClient.clients.update(
        { id: currentClient.id! },
        {
          clientId: currentClient.clientId,
          authorizationServicesEnabled: false,
          serviceAccountsEnabled: false,
        },
      );
    });

    it("create authorization scopes", async () => {
      scopes = (
        await Promise.all(
          resourceConfig.scopeNames.map(async (name) => {
            const result = await kcAdminClient.clients.createAuthorizationScope(
              { id: currentClient.id! },
              {
                name,
              },
            );
            expect(result).to.be.ok;
            return result;
          }),
        )
      ).sort((a, b) => (a.name < b.name ? -1 : 1));
    });

    it("list all authorization scopes", async () => {
      const result = await kcAdminClient.clients.listAllScopes({
        id: currentClient.id!,
      });
      expect(result.sort((a, b) => (a.name! < b.name! ? -1 : 1))).to.deep.equal(
        scopes,
      );
    });

    it("update authorization scope", async () => {
      const updatedScope = { ...scopes[0], displayName: "Hello" };
      await kcAdminClient.clients.updateAuthorizationScope(
        { id: currentClient.id!, scopeId: scopes[0].id! },
        updatedScope,
      );

      const fetchedScope = await kcAdminClient.clients.getAuthorizationScope({
        id: currentClient.id!,
        scopeId: scopes[0].id!,
      });

      expect(fetchedScope).to.deep.equal(updatedScope);
    });

    it("list all resources by scope", async () => {
      const result = await kcAdminClient.clients.listAllResourcesByScope({
        id: currentClient.id!,
        scopeId: scopes[0].id!,
      });
      expect(result).to.deep.equal([]);
    });

    it("list all permissions by scope", async () => {
      const result = await kcAdminClient.clients.listAllPermissionsByScope({
        id: currentClient.id!,
        scopeId: scopes[0].id!,
      });
      expect(result).to.deep.equal([]);
    });

    it("list permission scope", async () => {
      permission = await kcAdminClient.clients.createPermission(
        {
          id: currentClient.id!,
          type: "scope",
        },
        {
          name: permissionConfig.name,
          // @ts-ignore
          resources: [resource._id],
          policies: [policy.id!],
          scopes: scopes.map((scope) => scope.id!),
        },
      );

      const p = await kcAdminClient.clients.listPermissionScope({
        id: currentClient.id!,
        name: permissionConfig.name,
      });

      expect(p.length).to.be.eq(1);
      expect(p[0].name).to.be.eq(permissionConfig.name);
    });

    it("import resource", async () => {
      await kcAdminClient.clients.importResource(
        { id: currentClient.id! },
        {
          allowRemoteResourceManagement: true,
          policyEnforcementMode: "ENFORCING",
          resources: [],
          policies: [],
          scopes: [],
          decisionStrategy: "UNANIMOUS",
        },
      );
    });

    it("export resource", async () => {
      const result = await kcAdminClient.clients.exportResource({
        id: currentClient.id!,
      });

      expect(result.allowRemoteResourceManagement).to.be.equal(true);
      expect(result.resources?.length).to.be.equal(1);
    });

    it("create resource", async () => {
      resource = await kcAdminClient.clients.createResource(
        { id: currentClient.id! },
        {
          name: resourceConfig.name,
          type: resourceConfig.type,
          scopes,
        },
      );
      expect(resource).to.be.ok;
    });

    it("get resource", async () => {
      const r = await kcAdminClient.clients.getResource({
        id: currentClient.id!,
        resourceId: resource._id!,
      });
      expect(r).to.deep.equal(resource);
    });

    it("get resource server", async () => {
      const resourceServer = await kcAdminClient.clients.getResourceServer({
        id: currentClient.id!,
      });
      expect(resourceServer).to.be.ok;
      expect(resourceServer.clientId).to.be.equal(currentClient.id);

      resourceServer.decisionStrategy = "UNANIMOUS";
      await kcAdminClient.clients.updateResourceServer(
        { id: currentClient.id! },
        resourceServer,
      );
    });

    it("list permission by resource", async () => {
      const result = await kcAdminClient.clients.listPermissionsByResource({
        id: currentClient.id!,
        resourceId: resource._id!,
      });

      expect(result).to.be.ok;
    });

    it("list scopes by resource", async () => {
      const result = await kcAdminClient.clients.listScopesByResource({
        id: currentClient.id!,
        resourceName: resource._id!,
      });
      expect(result.sort((a, b) => (a.name < b.name ? -1 : 1))).to.deep.equal(
        scopes,
      );
    });

    it("list resources", async () => {
      const result = await kcAdminClient.clients.listResources({
        id: currentClient.id!,
      });
      expect(result).to.deep.include(resource);
    });

    it("update resource", async () => {
      resource.name = "foo";
      await kcAdminClient.clients.updateResource(
        {
          id: currentClient.id!,
          resourceId: resource._id!,
        },
        resource,
      );
      const result = await kcAdminClient.clients.getResource({
        id: currentClient.id!,
        resourceId: resource._id!,
      });

      expect(result.name).to.equal("foo");
    });

    it("create policy", async () => {
      policy = await kcAdminClient.clients.createPolicy(
        {
          id: currentClient.id!,
          type: policyConfig.type,
        },
        {
          name: policyConfig.name,
          logic: policyConfig.logic,
          users: [user.id!],
        },
      );
      expect(policy).to.be.ok;
    });

    it("policy list dependencies", async () => {
      const dependencies = await kcAdminClient.clients.listDependentPolicies({
        id: currentClient.id!,
        policyId: policy.id!,
      });
      expect(dependencies).to.be.ok;
    });

    it("create permission", async () => {
      permission = await kcAdminClient.clients.createPermission(
        {
          id: currentClient.id!,
          type: "scope",
        },
        {
          name: permissionConfig.name,
          logic: permissionConfig.logic,
          // @ts-ignore
          resources: [resource._id],
          policies: [policy.id!],
          scopes: scopes.map((scope) => scope.id!),
        },
      );

      const p = await kcAdminClient.clients.findPermissions({
        id: currentClient.id!,
        name: permissionConfig.name,
      });

      expect(p.length).to.be.eq(1);
      expect(p[0].logic).to.be.eq(permissionConfig.logic);
    });

    it("get associated scopes for permission", async () => {
      const result = await kcAdminClient.clients.getAssociatedScopes({
        id: currentClient.id!,
        permissionId: permission.id!,
      });
      expect(result.sort((a, b) => (a.name < b.name ? -1 : 1))).to.deep.equal(
        scopes,
      );
    });

    it("get associated policies for permission", async () => {
      const result = await kcAdminClient.clients.getAssociatedPolicies({
        id: currentClient.id!,
        permissionId: permission.id!,
      });

      expect(result.length).to.be.eq(1);
      expect(result[0].id).to.be.eq(policy.id);
    });

    it("get associated resources for permission", async () => {
      const result = await kcAdminClient.clients.getAssociatedResources({
        id: currentClient.id!,
        permissionId: permission.id!,
      });
      expect(result).to.deep.equal([
        {
          _id: resource._id,
          name: resource.name,
        },
      ]);
    });

    it("list policy providers", async () => {
      const result = await kcAdminClient.clients.listPolicyProviders({
        id: currentClient.id!,
      });
      expect(result).to.be.ok;
    });

    it.skip("Enable fine grained permissions", async () => {
      const permission = await kcAdminClient.clients.updateFineGrainPermission(
        { id: currentClient.id! },
        { enabled: true },
      );
      expect(permission).to.include({
        enabled: true,
      });
    });

    it.skip("List fine grained permissions for this client", async () => {
      const permissions = (await kcAdminClient.clients.listFineGrainPermissions(
        { id: currentClient.id! },
      ))!;

      expect(permissions.scopePermissions).to.be.an("object");
    });
  });
});
