// tslint:disable:no-unused-expression
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import type ClientRepresentation from "../src/defs/clientRepresentation.js";
import type ClientScopeRepresentation from "../src/defs/clientScopeRepresentation.js";
import type ProtocolMapperRepresentation from "../src/defs/protocolMapperRepresentation.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Client Scopes", () => {
  let kcAdminClient: KeycloakAdminClient;
  let currentClientScope: ClientScopeRepresentation;
  let currentClientScopeName: string;
  let currentClient: ClientRepresentation;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);
  });

  beforeEach(async () => {
    currentClientScopeName = "best-of-the-bests-scope";
    await kcAdminClient.clientScopes.create({
      name: currentClientScopeName,
    });
    currentClientScope = (await kcAdminClient.clientScopes.findOneByName({
      name: currentClientScopeName,
    }))!;
  });

  afterEach(async () => {
    // cleanup default client scopes
    try {
      await kcAdminClient.clientScopes.delDefaultClientScope({
        id: currentClientScope.id!,
      });
    } catch {
      // ignore
    }

    // cleanup optional client scopes
    try {
      await kcAdminClient.clientScopes.delDefaultOptionalClientScope({
        id: currentClientScope.id!,
      });
    } catch {
      // ignore
    }

    // cleanup client scopes
    try {
      await kcAdminClient.clientScopes.delByName({
        name: currentClientScopeName,
      });
    } catch {
      // ignore
    }
  });

  it("list client scopes", async () => {
    const scopes = await kcAdminClient.clientScopes.find();
    expect(scopes).to.be.ok;
  });

  it("create client scope and get by name", async () => {
    // ensure that the scope does not exist
    try {
      await kcAdminClient.clientScopes.delByName({
        name: currentClientScopeName,
      });
    } catch {
      // ignore
    }

    await kcAdminClient.clientScopes.create({
      name: currentClientScopeName,
    });

    const scope = (await kcAdminClient.clientScopes.findOneByName({
      name: currentClientScopeName,
    }))!;
    expect(scope).to.be.ok;
    expect(scope.name).to.equal(currentClientScopeName);
  });

  it("create client scope and return id", async () => {
    // ensure that the scope does not exist
    try {
      await kcAdminClient.clientScopes.delByName({
        name: currentClientScopeName,
      });
    } catch {
      // ignore
    }

    const { id } = await kcAdminClient.clientScopes.create({
      name: currentClientScopeName,
    });

    const scope = (await kcAdminClient.clientScopes.findOne({
      id,
    }))!;
    expect(scope).to.be.ok;
    expect(scope.name).to.equal(currentClientScopeName);
  });

  it("find scope by id", async () => {
    const scope = await kcAdminClient.clientScopes.findOne({
      id: currentClientScope.id!,
    });
    expect(scope).to.be.ok;
    expect(scope).to.eql(currentClientScope);
  });

  it("find scope by name", async () => {
    const scope = (await kcAdminClient.clientScopes.findOneByName({
      name: currentClientScopeName,
    }))!;
    expect(scope).to.be.ok;
    expect(scope.name).to.eql(currentClientScopeName);
  });

  it("return null if scope not found by id", async () => {
    const scope = await kcAdminClient.clientScopes.findOne({
      id: "I do not exist",
    });
    expect(scope).to.be.null;
  });

  it("return null if scope not found by name", async () => {
    const scope = await kcAdminClient.clientScopes.findOneByName({
      name: "I do not exist",
    });
    expect(scope).to.be.undefined;
  });

  it.skip("update client scope", async () => {
    const { id, description: oldDescription } = currentClientScope;
    const description = "This scope is totally awesome.";

    await kcAdminClient.clientScopes.update({ id: id! }, { description });
    const updatedScope = (await kcAdminClient.clientScopes.findOne({
      id: id!,
    }))!;
    expect(updatedScope).to.be.ok;
    expect(updatedScope).not.to.eql(currentClientScope);
    expect(updatedScope.description).to.eq(description);
    expect(updatedScope.description).not.to.eq(oldDescription);
  });

  it("delete single client scope by id", async () => {
    await kcAdminClient.clientScopes.del({
      id: currentClientScope.id!,
    });
    const scope = await kcAdminClient.clientScopes.findOne({
      id: currentClientScope.id!,
    });
    expect(scope).not.to.be.ok;
  });

  it("delete single client scope by name", async () => {
    await kcAdminClient.clientScopes.delByName({
      name: currentClientScopeName,
    });
    const scope = await kcAdminClient.clientScopes.findOneByName({
      name: currentClientScopeName,
    });
    expect(scope).not.to.be.ok;
  });

  describe("default client scope", () => {
    it("list default client scopes", async () => {
      const defaultClientScopes =
        await kcAdminClient.clientScopes.listDefaultClientScopes();
      expect(defaultClientScopes).to.be.ok;
    });

    it("add default client scope", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addDefaultClientScope({ id: id! });

      const defaultClientScopeList =
        await kcAdminClient.clientScopes.listDefaultClientScopes();
      const defaultClientScope = defaultClientScopeList.find(
        (scope) => scope.id === id,
      )!;

      expect(defaultClientScope).to.be.ok;
      expect(defaultClientScope.id).to.equal(currentClientScope.id);
      expect(defaultClientScope.name).to.equal(currentClientScope.name);
    });

    it("delete default client scope", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addDefaultClientScope({ id: id! });

      await kcAdminClient.clientScopes.delDefaultClientScope({ id: id! });

      const defaultClientScopeList =
        await kcAdminClient.clientScopes.listDefaultClientScopes();
      const defaultClientScope = defaultClientScopeList.find(
        (scope) => scope.id === id,
      );

      expect(defaultClientScope).not.to.be.ok;
    });
  });

  describe("default optional client scopes", () => {
    it("list default optional client scopes", async () => {
      const defaultOptionalClientScopes =
        await kcAdminClient.clientScopes.listDefaultOptionalClientScopes();
      expect(defaultOptionalClientScopes).to.be.ok;
    });

    it("add default optional client scope", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addDefaultOptionalClientScope({
        id: id!,
      });

      const defaultOptionalClientScopeList =
        await kcAdminClient.clientScopes.listDefaultOptionalClientScopes();
      const defaultOptionalClientScope = defaultOptionalClientScopeList.find(
        (scope) => scope.id === id,
      )!;

      expect(defaultOptionalClientScope).to.be.ok;
      expect(defaultOptionalClientScope.id).to.eq(currentClientScope.id);
      expect(defaultOptionalClientScope.name).to.eq(currentClientScope.name);
    });

    it("delete default optional client scope", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addDefaultOptionalClientScope({
        id: id!,
      });
      await kcAdminClient.clientScopes.delDefaultOptionalClientScope({
        id: id!,
      });

      const defaultOptionalClientScopeList =
        await kcAdminClient.clientScopes.listDefaultOptionalClientScopes();
      const defaultOptionalClientScope = defaultOptionalClientScopeList.find(
        (scope) => scope.id === id,
      );

      expect(defaultOptionalClientScope).not.to.be.ok;
    });
  });

  describe("protocol mappers", () => {
    let dummyMapper: ProtocolMapperRepresentation;

    beforeEach(() => {
      dummyMapper = {
        name: "mapping-maps-mapper",
        protocol: "openid-connect",
        protocolMapper: "oidc-audience-mapper",
      };
    });

    afterEach(async () => {
      try {
        const { id } = currentClientScope;
        const { id: mapperId } =
          (await kcAdminClient.clientScopes.findProtocolMapperByName({
            id: id!,
            name: dummyMapper.name!,
          }))!;
        await kcAdminClient.clientScopes.delProtocolMapper({
          id: id!,
          mapperId: mapperId!,
        });
      } catch {
        // ignore
      }
    });

    it("list protocol mappers", async () => {
      const { id } = currentClientScope;
      const mapperList = await kcAdminClient.clientScopes.listProtocolMappers({
        id: id!,
      });
      expect(mapperList).to.be.ok;
    });

    it("add multiple protocol mappers", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addMultipleProtocolMappers({ id: id! }, [
        dummyMapper,
      ]);

      const mapper = (await kcAdminClient.clientScopes.findProtocolMapperByName(
        {
          id: id!,
          name: dummyMapper.name!,
        },
      ))!;
      expect(mapper).to.be.ok;
      expect(mapper.protocol).to.eq(dummyMapper.protocol);
      expect(mapper.protocolMapper).to.eq(dummyMapper.protocolMapper);
    });

    it("add single protocol mapper", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addProtocolMapper(
        { id: id! },
        dummyMapper,
      );

      const mapper = (await kcAdminClient.clientScopes.findProtocolMapperByName(
        {
          id: id!,
          name: dummyMapper.name!,
        },
      ))!;
      expect(mapper).to.be.ok;
      expect(mapper.protocol).to.eq(dummyMapper.protocol);
      expect(mapper.protocolMapper).to.eq(dummyMapper.protocolMapper);
    });

    it("find protocol mapper by id", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addProtocolMapper(
        { id: id! },
        dummyMapper,
      );

      const { id: mapperId } =
        (await kcAdminClient.clientScopes.findProtocolMapperByName({
          id: id!,
          name: dummyMapper.name!,
        }))!;

      const mapper = await kcAdminClient.clientScopes.findProtocolMapper({
        id: id!,
        mapperId: mapperId!,
      });

      expect(mapper).to.be.ok;
      expect(mapper?.id).to.eql(mapperId);
    });

    it("find protocol mapper by name", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addProtocolMapper(
        { id: id! },
        dummyMapper,
      );

      const mapper = (await kcAdminClient.clientScopes.findProtocolMapperByName(
        {
          id: id!,
          name: dummyMapper.name!,
        },
      ))!;

      expect(mapper).to.be.ok;
      expect(mapper.name).to.eql(dummyMapper.name);
    });

    it("find protocol mappers by protocol", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addProtocolMapper(
        { id: id! },
        dummyMapper,
      );

      const mapperList =
        await kcAdminClient.clientScopes.findProtocolMappersByProtocol({
          id: id!,
          protocol: dummyMapper.protocol!,
        });

      expect(mapperList).to.be.ok;
      expect(mapperList.length).to.be.gte(1);

      const mapper = mapperList.find((item) => item.name === dummyMapper.name);
      expect(mapper).to.be.ok;
    });

    it("update protocol mapper", async () => {
      const { id } = currentClientScope;

      dummyMapper.config = { "access.token.claim": "true" };
      await kcAdminClient.clientScopes.addProtocolMapper(
        { id: id! },
        dummyMapper,
      );
      const mapper = (await kcAdminClient.clientScopes.findProtocolMapperByName(
        {
          id: id!,
          name: dummyMapper.name!,
        },
      ))!;

      expect(mapper.config!["access.token.claim"]).to.eq("true");

      mapper.config = { "access.token.claim": "false" };

      await kcAdminClient.clientScopes.updateProtocolMapper(
        { id: id!, mapperId: mapper.id! },
        mapper,
      );

      const updatedMapper =
        (await kcAdminClient.clientScopes.findProtocolMapperByName({
          id: id!,
          name: dummyMapper.name!,
        }))!;

      expect(updatedMapper.config!["access.token.claim"]).to.eq("false");
    });

    it("delete protocol mapper", async () => {
      const { id } = currentClientScope;
      await kcAdminClient.clientScopes.addProtocolMapper(
        { id: id! },
        dummyMapper,
      );

      const { id: mapperId } =
        (await kcAdminClient.clientScopes.findProtocolMapperByName({
          id: id!,
          name: dummyMapper.name!,
        }))!;

      await kcAdminClient.clientScopes.delProtocolMapper({
        id: id!,
        mapperId: mapperId!,
      });

      const mapper = await kcAdminClient.clientScopes.findProtocolMapperByName({
        id: id!,
        name: dummyMapper.name!,
      });

      expect(mapper).not.to.be.ok;
    });
  });

  describe("scope mappings", () => {
    it("list client and realm scope mappings", async () => {
      const { id } = currentClientScope;
      const scopes = await kcAdminClient.clientScopes.listScopeMappings({
        id: id!,
      });
      expect(scopes).to.be.ok;
    });

    describe("client", () => {
      const dummyClientId = "scopeMappings-dummy";
      const dummyRoleName = "scopeMappingsRole-dummy";

      beforeEach(async () => {
        const { id } = await kcAdminClient.clients.create({
          clientId: dummyClientId,
        });
        currentClient = (await kcAdminClient.clients.findOne({
          id,
        }))!;

        await kcAdminClient.clients.createRole({
          id,
          name: dummyRoleName,
        });
      });

      afterEach(async () => {
        const { id } = currentClient;
        await kcAdminClient.clients.delRole({
          id: id!,
          roleName: dummyRoleName,
        });
        await kcAdminClient.clients.del({ id: id! });
      });

      it("add scope mappings", async () => {
        const { id } = currentClientScope;
        const { id: clientUniqueId } = currentClient;

        const availableRoles =
          await kcAdminClient.clientScopes.listAvailableClientScopeMappings({
            id: id!,
            client: clientUniqueId!,
          });

        const filteredRoles = availableRoles.filter((role) => !role.composite);

        await kcAdminClient.clientScopes.addClientScopeMappings(
          {
            id: id!,
            client: clientUniqueId!,
          },
          filteredRoles,
        );

        const roles = await kcAdminClient.clientScopes.listClientScopeMappings({
          id: id!,
          client: clientUniqueId!,
        });

        expect(roles).to.be.ok;
        expect(roles).to.be.eql(filteredRoles);
      });

      it("list scope mappings", async () => {
        const { id } = currentClientScope;
        const { id: clientUniqueId } = currentClient;
        const roles = await kcAdminClient.clientScopes.listClientScopeMappings({
          id: id!,
          client: clientUniqueId!,
        });
        expect(roles).to.be.ok;
      });

      it("list available scope mappings", async () => {
        const { id } = currentClientScope;
        const { id: clientUniqueId } = currentClient;
        const roles =
          await kcAdminClient.clientScopes.listAvailableClientScopeMappings({
            id: id!,
            client: clientUniqueId!,
          });
        expect(roles).to.be.ok;
      });

      it("list composite scope mappings", async () => {
        const { id } = currentClientScope;
        const { id: clientUniqueId } = currentClient;
        const roles =
          await kcAdminClient.clientScopes.listCompositeClientScopeMappings({
            id: id!,
            client: clientUniqueId!,
          });
        expect(roles).to.be.ok;
      });

      it("delete scope mappings", async () => {
        const { id } = currentClientScope;
        const { id: clientUniqueId } = currentClient;

        const rolesBefore =
          await kcAdminClient.clientScopes.listClientScopeMappings({
            id: id!,
            client: clientUniqueId!,
          });

        await kcAdminClient.clientScopes.delClientScopeMappings(
          {
            id: id!,
            client: clientUniqueId!,
          },
          rolesBefore,
        );

        const rolesAfter =
          await kcAdminClient.clientScopes.listClientScopeMappings({
            id: id!,
            client: clientUniqueId!,
          });

        expect(rolesAfter).to.be.ok;
        expect(rolesAfter).to.eql([]);
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
        const { id } = currentClientScope;

        const availableRoles =
          await kcAdminClient.clientScopes.listAvailableRealmScopeMappings({
            id: id!,
          });

        const filteredRoles = availableRoles.filter((role) => !role.composite);

        await kcAdminClient.clientScopes.addRealmScopeMappings(
          { id: id! },
          filteredRoles,
        );

        const roles = await kcAdminClient.clientScopes.listRealmScopeMappings({
          id: id!,
        });

        expect(roles).to.be.ok;
        expect(roles).to.include.deep.members(filteredRoles);
      });

      it("list scope mappings", async () => {
        const { id } = currentClientScope;
        const roles = await kcAdminClient.clientScopes.listRealmScopeMappings({
          id: id!,
        });
        expect(roles).to.be.ok;
      });

      it("list available scope mappings", async () => {
        const { id } = currentClientScope;
        const roles =
          await kcAdminClient.clientScopes.listAvailableRealmScopeMappings({
            id: id!,
          });
        expect(roles).to.be.ok;
      });

      it("list composite scope mappings", async () => {
        const { id } = currentClientScope;
        const roles =
          await kcAdminClient.clientScopes.listCompositeRealmScopeMappings({
            id: id!,
          });
        expect(roles).to.be.ok;
      });

      it("delete scope mappings", async () => {
        const { id } = currentClientScope;

        const rolesBefore =
          await kcAdminClient.clientScopes.listRealmScopeMappings({
            id: id!,
          });

        await kcAdminClient.clientScopes.delRealmScopeMappings(
          {
            id: id!,
          },
          rolesBefore,
        );

        const rolesAfter =
          await kcAdminClient.clientScopes.listRealmScopeMappings({
            id: id!,
          });

        expect(rolesAfter).to.be.ok;
        expect(rolesAfter).to.eql([]);
      });
    });
  });
});
