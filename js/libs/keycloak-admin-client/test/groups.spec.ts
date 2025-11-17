// tslint:disable:no-unused-expression
import { faker } from "@faker-js/faker";
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import type ClientRepresentation from "../src/defs/clientRepresentation.js";
import type GroupRepresentation from "../src/defs/groupRepresentation.js";
import type RoleRepresentation from "../src/defs/roleRepresentation.js";
import type { SubGroupQuery } from "../src/resources/groups.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Groups", () => {
  let kcAdminClient: KeycloakAdminClient;
  let currentClient: ClientRepresentation;
  let currentGroup: GroupRepresentation;
  let currentRole: RoleRepresentation;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);
    // initialize group
    const group = await kcAdminClient.groups.create({
      name: "cool-group",
    });
    expect(group.id).to.be.ok;
    currentGroup = (await kcAdminClient.groups.findOne({ id: group.id }))!;
  });

  after(async () => {
    const groupId = currentGroup.id;
    await kcAdminClient.groups.del({
      id: groupId!,
    });

    const group = await kcAdminClient.groups.findOne({
      id: groupId!,
    });
    expect(group).to.be.null;
  });

  it("list groups", async () => {
    const groups = await kcAdminClient.groups.find();
    expect(groups).to.be.ok;
  });

  it("count groups", async () => {
    const result = await kcAdminClient.groups.count();
    expect(result.count).to.eq(1);
  });

  it("count groups with filter", async () => {
    let result = await kcAdminClient.groups.count({ search: "fake-group" });
    expect(result.count).to.eq(0);

    result = await kcAdminClient.groups.count({ search: "cool-group" });
    expect(result.count).to.eq(1);
  });

  it("get single groups", async () => {
    const groupId = currentGroup.id;
    const group = await kcAdminClient.groups.findOne({
      id: groupId!,
    });
    // get group from id will contains more fields than listing api
    expect(group).to.deep.include(currentGroup);
  });

  it("update single groups", async () => {
    const groupId = currentGroup.id;
    await kcAdminClient.groups.update(
      { id: groupId! },
      { name: "another-group-name", description: "another-group-description" },
    );

    const group = await kcAdminClient.groups.findOne({
      id: groupId!,
    });
    expect(group).to.include({
      name: "another-group-name",
    });
  });

  it("list subgroups", async () => {
    if (currentGroup.id) {
      const args: SubGroupQuery = {
        parentId: currentGroup!.id,
        first: 0,
        max: 10,
        briefRepresentation: false,
      };
      const groups = await kcAdminClient.groups.listSubGroups(args);
      expect(groups.length).to.equal(1);
    }
  });

  /**
   * Role mappings
   */
  describe("role-mappings", () => {
    before(async () => {
      // create new role
      const roleName = faker.internet.username();
      const { roleName: createdRoleName } = await kcAdminClient.roles.create({
        name: roleName,
      });
      expect(createdRoleName).to.be.equal(roleName);
      const role = await kcAdminClient.roles.findOneByName({
        name: roleName,
      });
      currentRole = role!;
    });

    after(async () => {
      await kcAdminClient.roles.delByName({ name: currentRole.name! });
    });

    it("add a role to group", async () => {
      // add role-mappings with role id
      await kcAdminClient.groups.addRealmRoleMappings({
        id: currentGroup.id!,

        // at least id and name should appear
        roles: [
          {
            id: currentRole.id!,
            name: currentRole.name!,
          },
        ],
      });
    });

    it("list available role-mappings", async () => {
      const roles = await kcAdminClient.groups.listAvailableRealmRoleMappings({
        id: currentGroup.id!,
      });

      // admin, create-realm, offline_access, uma_authorization
      expect(roles.length).to.be.least(4);
    });

    it("list role-mappings", async () => {
      const { realmMappings } = await kcAdminClient.groups.listRoleMappings({
        id: currentGroup.id!,
      });

      expect(realmMappings).to.be.ok;
      // currentRole will have an empty `attributes`, but role-mappings do not
      expect(currentRole).to.deep.include(realmMappings![0]);
    });

    it("list realm role-mappings of group", async () => {
      const roles = await kcAdminClient.groups.listRealmRoleMappings({
        id: currentGroup.id!,
      });
      // currentRole will have an empty `attributes`, but role-mappings do not
      expect(currentRole).to.deep.include(roles[0]);
    });

    it("list realm composite role-mappings of group", async () => {
      const roles = await kcAdminClient.groups.listCompositeRealmRoleMappings({
        id: currentGroup.id!,
      });
      // todo: add data integrity check later
      expect(roles).to.be.ok;
    });

    it("del realm role-mappings from group", async () => {
      await kcAdminClient.groups.delRealmRoleMappings({
        id: currentGroup.id!,
        roles: [
          {
            id: currentRole.id!,
            name: currentRole.name!,
          },
        ],
      });

      const roles = await kcAdminClient.groups.listRealmRoleMappings({
        id: currentGroup.id!,
      });
      expect(roles).to.be.empty;
    });
  });

  /**
   * client Role mappings
   */
  describe("client role-mappings", () => {
    before(async () => {
      // create new client
      const clientId = faker.internet.username();
      await kcAdminClient.clients.create({
        clientId,
      });

      const clients = await kcAdminClient.clients.find({ clientId });
      expect(clients[0]).to.be.ok;
      currentClient = clients[0];

      // create new client role
      const roleName = faker.internet.username();
      await kcAdminClient.clients.createRole({
        id: currentClient.id,
        name: roleName,
      });

      // assign to currentRole
      currentRole = await kcAdminClient.clients.findRole({
        id: currentClient.id!,
        roleName,
      });
    });

    after(async () => {
      await kcAdminClient.clients.delRole({
        id: currentClient.id!,
        roleName: currentRole.name!,
      });
      await kcAdminClient.clients.del({ id: currentClient.id! });
    });

    it("add a client role to group", async () => {
      // add role-mappings with role id
      await kcAdminClient.groups.addClientRoleMappings({
        id: currentGroup.id!,
        clientUniqueId: currentClient.id!,

        // at least id and name should appear
        roles: [
          {
            id: currentRole.id!,
            name: currentRole.name!,
          },
        ],
      });
    });

    it("list available client role-mappings for group", async () => {
      const roles = await kcAdminClient.groups.listAvailableClientRoleMappings({
        id: currentGroup.id!,
        clientUniqueId: currentClient.id!,
      });

      expect(roles).to.be.empty;
    });

    it("list client role-mappings of group", async () => {
      const roles = await kcAdminClient.groups.listClientRoleMappings({
        id: currentGroup.id!,
        clientUniqueId: currentClient.id!,
      });

      // currentRole will have an empty `attributes`, but role-mappings do not
      expect(currentRole).to.deep.include(roles[0]);
    });

    it("list composite client role-mappings for group", async () => {
      const roles = await kcAdminClient.groups.listCompositeClientRoleMappings({
        id: currentGroup.id!,
        clientUniqueId: currentClient.id!,
      });

      expect(roles).to.be.ok;
    });

    it("del client role-mappings from group", async () => {
      const roleName = faker.internet.username();
      await kcAdminClient.clients.createRole({
        id: currentClient.id,
        name: roleName,
      });
      const role = await kcAdminClient.clients.findRole({
        id: currentClient.id!,
        roleName,
      });

      // delete the created role
      await kcAdminClient.groups.delClientRoleMappings({
        id: currentGroup.id!,
        clientUniqueId: currentClient.id!,
        roles: [
          {
            id: role.id!,
            name: role.name!,
          },
        ],
      });

      // check if mapping is successfully deleted
      const roles = await kcAdminClient.groups.listClientRoleMappings({
        id: currentGroup.id!,
        clientUniqueId: currentClient.id!,
      });

      // should only left the one we added in the previous test
      expect(roles.length).to.be.eql(1);
    });
  });
});
