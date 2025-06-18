// tslint:disable:no-unused-expression
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import type ClientRepresentation from "../src/defs/clientRepresentation.js";
import type RoleRepresentation from "../src/defs/roleRepresentation.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Roles", () => {
  let client: KeycloakAdminClient;
  let currentRole: RoleRepresentation;

  before(async () => {
    client = new KeycloakAdminClient();
    await client.auth(credentials);
  });

  after(async () => {
    // delete the currentRole with id
    await client.roles.delById({
      id: currentRole.id!,
    });
  });

  it("list roles", async () => {
    const roles = await client.roles.find();
    expect(roles).to.be.ok;
  });

  it("create roles and get by name", async () => {
    const roleName = "cool-role";
    const createdRole = await client.roles.create({
      name: roleName,
    });

    expect(createdRole.roleName).to.be.equal(roleName);
    const role = await client.roles.findOneByName({ name: roleName });
    expect(role).to.be.ok;
    currentRole = role!;
  });

  it("get single roles by id", async () => {
    const roleId = currentRole.id;
    const role = await client.roles.findOneById({
      id: roleId!,
    });
    expect(role).to.deep.include(currentRole);
  });

  it("update single role by name & by id", async () => {
    await client.roles.updateByName(
      { name: currentRole.name! },
      {
        // dont know why if role name not exist in payload, role name will be overriden with empty string
        // todo: open an issue on keycloak
        name: "cool-role",
        description: "cool",
      },
    );

    const role = await client.roles.findOneByName({
      name: currentRole.name!,
    });
    expect(role).to.include({
      description: "cool",
    });

    await client.roles.updateById(
      { id: currentRole.id! },
      {
        description: "another description",
      },
    );

    const roleById = await client.roles.findOneById({
      id: currentRole.id!,
    });
    expect(roleById).to.include({
      description: "another description",
    });
  });

  it("delete single roles by id", async () => {
    await client.roles.create({
      name: "for-delete",
    });

    await client.roles.delByName({
      name: "for-delete",
    });

    const roleDelByName = await client.roles.findOneByName({
      name: "for-delete",
    });
    expect(roleDelByName).to.be.null;
  });

  it("get users with role by name in realm", async () => {
    const users = await client.roles.findUsersWithRole({
      name: "admin",
    });
    expect(users).to.be.ok;
    expect(users).to.be.an("array");
  });

  it.skip("Enable fine grained permissions", async () => {
    const permission = await client.roles.updatePermission(
      { id: currentRole.id! },
      { enabled: true },
    );
    expect(permission).to.include({
      enabled: true,
    });
  });

  it.skip("List fine grained permissions for this role", async () => {
    const permissions = (await client.roles.listPermissions({
      id: currentRole.id!,
    }))!;

    expect(permissions.scopePermissions).to.be.an("object");
  });

  describe("Composite roles", () => {
    const compositeRoleName = "compositeRole";
    let compositeRole: RoleRepresentation;

    beforeEach(async () => {
      await client.roles.create({
        name: compositeRoleName,
      });
      compositeRole = (await client.roles.findOneByName({
        name: compositeRoleName,
      }))!;
      await client.roles.createComposite({ roleId: currentRole.id! }, [
        compositeRole,
      ]);
    });

    afterEach(async () => {
      await client.roles.delByName({
        name: compositeRoleName,
      });
    });

    it("make the role a composite role by associating some child roles", async () => {
      const children = await client.roles.getCompositeRoles({
        id: currentRole.id!,
      });

      // attributes on the composite role are empty and when fetched not there.
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { attributes, ...rest } = compositeRole;
      expect(children).to.be.eql([rest]);
    });

    it("search for composite roles", async () => {
      const children = await client.roles.getCompositeRoles({
        id: currentRole.id!,
        search: "not",
      });

      expect(children).to.be.an("array").that.is.length(0);
    });

    it("delete composite roles", async () => {
      await client.roles.delCompositeRoles({ id: currentRole.id! }, [
        compositeRole,
      ]);
      const children = await client.roles.getCompositeRoles({
        id: currentRole.id!,
      });

      expect(children).to.be.an("array").that.is.empty;
    });

    describe("Get composite roles for client and realm", () => {
      let createdClient: ClientRepresentation;
      let clientRole: RoleRepresentation;
      before(async () => {
        createdClient = await client.clients.create({ clientId: "test" });
        const clientRoleName = "clientRole";
        await client.clients.createRole({
          id: createdClient.id,
          name: clientRoleName,
        });
        clientRole = await client.clients.findRole({
          id: createdClient.id!,
          roleName: clientRoleName,
        });

        await client.roles.createComposite({ roleId: currentRole.id! }, [
          clientRole,
        ]);
      });

      after(async () => {
        await client.clients.del({ id: createdClient.id! });
      });

      it("get composite role for the realm", async () => {
        const realmChildren = await client.roles.getCompositeRolesForRealm({
          id: currentRole.id!,
        });
        const children = await client.roles.getCompositeRoles({
          id: currentRole.id!,
        });

        delete compositeRole.attributes;
        expect(realmChildren).to.be.eql([compositeRole]);

        expect(children).to.be.an("array").that.is.length(2);
      });

      it("get composite for the client", async () => {
        const clientChildren = await client.roles.getCompositeRolesForClient({
          id: currentRole.id!,
          clientId: createdClient.id!,
        });

        delete clientRole.attributes;
        expect(clientChildren).to.be.eql([clientRole]);
      });
    });
  });
});
