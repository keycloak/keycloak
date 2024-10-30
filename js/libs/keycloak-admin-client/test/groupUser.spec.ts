// tslint:disable:no-unused-expression
import { faker } from "@faker-js/faker";
import * as chai from "chai";
import { omit, pick } from "lodash-es";
import { KeycloakAdminClient } from "../src/client.js";
import type ClientRepresentation from "../src/defs/clientRepresentation.js";
import type GroupRepresentation from "../src/defs/groupRepresentation.js";
import type PolicyRepresentation from "../src/defs/policyRepresentation.js";
import { DecisionStrategy, Logic } from "../src/defs/policyRepresentation.js";
import type UserRepresentation from "../src/defs/userRepresentation.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Group user integration", () => {
  let kcAdminClient: KeycloakAdminClient;
  let currentGroup: GroupRepresentation;
  let currentUser: UserRepresentation;
  let managementClient: ClientRepresentation;
  let currentUserPolicy: PolicyRepresentation;
  let currentPolicy: PolicyRepresentation;

  before(async () => {
    const groupName = faker.internet.username();
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);
    // create group
    const group = await kcAdminClient.groups.create({
      name: groupName,
    });
    currentGroup = (await kcAdminClient.groups.findOne({ id: group.id }))!;

    // create user
    const username = faker.internet.username();
    const user = await kcAdminClient.users.create({
      username,
      email: "test@keycloak.org",
      enabled: true,
    });
    currentUser = (await kcAdminClient.users.findOne({ id: user.id }))!;
  });

  after(async () => {
    await kcAdminClient.groups.del({
      id: currentGroup.id!,
    });
    await kcAdminClient.users.del({
      id: currentUser.id!,
    });
  });

  it("should list user's group and expect empty", async () => {
    const groups = await kcAdminClient.users.listGroups({
      id: currentUser.id!,
    });
    expect(groups).to.be.eql([]);
  });

  it("should add user to group", async () => {
    await kcAdminClient.users.addToGroup({
      id: currentUser.id!,
      groupId: currentGroup.id!,
    });

    const groups = await kcAdminClient.users.listGroups({
      id: currentUser.id!,
    });
    // expect id,name,path to be the same
    expect(groups[0]).to.be.eql(pick(currentGroup, ["id", "name", "path"]));
  });

  it("should list members using group api", async () => {
    const members = await kcAdminClient.groups.listMembers({
      id: currentGroup.id!,
    });
    // access will not returned from member api
    expect(members[0]).to.be.eql(omit(currentUser, ["access"]));
  });

  it("should remove user from group", async () => {
    await kcAdminClient.users.delFromGroup({
      id: currentUser.id!,
      groupId: currentGroup.id!,
    });

    const groups = await kcAdminClient.users.listGroups({
      id: currentUser.id!,
    });
    expect(groups).to.be.eql([]);
  });

  /**
   * Authorization permissions
   */
  describe.skip("authorization permissions", () => {
    before(async () => {
      const clients = await kcAdminClient.clients.find();
      managementClient = clients.find(
        (client) => client.clientId === "master-realm",
      )!;
    });
    after(async () => {
      await kcAdminClient.clients.delPolicy({
        id: managementClient.id!,
        policyId: currentUserPolicy.id!,
      });
    });

    it("Enable permissions", async () => {
      const permission = await kcAdminClient.groups.updatePermission(
        { id: currentGroup.id! },
        { enabled: true },
      );
      expect(permission).to.include({
        enabled: true,
      });
    });

    it("list of users in policy management", async () => {
      const userPolicyData: PolicyRepresentation = {
        type: "user",
        logic: Logic.POSITIVE,
        decisionStrategy: DecisionStrategy.UNANIMOUS,
        name: `policy.manager.${currentGroup.id}`,
        users: [currentUser.id!],
      };
      currentUserPolicy = await kcAdminClient.clients.createPolicy(
        { id: managementClient.id!, type: userPolicyData.type! },
        userPolicyData,
      );

      expect(currentUserPolicy).to.include({
        type: "user",
        logic: Logic.POSITIVE,
        decisionStrategy: DecisionStrategy.UNANIMOUS,
        name: `policy.manager.${currentGroup.id}`,
      });
    });

    it("list the roles available for this group", async () => {
      const permissions = (await kcAdminClient.groups.listPermissions({
        id: currentGroup.id!,
      }))!;

      expect(permissions.scopePermissions).to.be.an("object");

      const scopes = (await kcAdminClient.clients.listScopesByResource({
        id: managementClient.id!,
        resourceName: permissions.resource!,
      }))!;

      const policies = await kcAdminClient.clients.listPolicies({
        id: managementClient.id,
        resource: permissions.resource,
        max: 2,
      });
      expect(policies).to.have.length(2);

      expect(scopes).to.have.length(5);

      // Search for the id of the management role
      const roleId = scopes.find((scope) => scope.name === "manage")!.id;

      const userPolicy = await kcAdminClient.clients.findPolicyByName({
        id: managementClient.id!,
        name: `policy.manager.${currentGroup.id}`,
      });

      expect(userPolicy).to.deep.include({
        name: `policy.manager.${currentGroup.id}`,
      });

      // Update of the role with the above modifications
      const policyData: PolicyRepresentation = {
        id: permissions.scopePermissions!.manage!,
        name: `manage.permission.group.${currentGroup.id}`,
        type: "scope",
        logic: Logic.POSITIVE,
        decisionStrategy: DecisionStrategy.UNANIMOUS,
        resources: [permissions.resource!],
        scopes: [roleId],
        policies: [userPolicy.id!],
      };
      await kcAdminClient.clients.updatePermission(
        {
          id: managementClient.id!,
          permissionId: permissions.scopePermissions!.manage,
          type: "scope",
        },
        policyData,
      );
      currentPolicy = (await kcAdminClient.clients.findOnePermission({
        id: managementClient.id!,
        permissionId: permissions.scopePermissions!.manage,
        type: "scope",
      }))!;
      expect(currentPolicy).to.deep.include({
        id: permissions.scopePermissions!.manage,
        name: `manage.permission.group.${currentGroup.id}`,
        type: "scope",
        logic: Logic.POSITIVE,
        decisionStrategy: DecisionStrategy.UNANIMOUS,
      });
    });
  });
});
