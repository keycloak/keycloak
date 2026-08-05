// tslint:disable:no-unused-expression
import { faker } from "@faker-js/faker";
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Organizations", () => {
  let kcAdminClient: KeycloakAdminClient;

  before(async () => {
    kcAdminClient = new KeycloakAdminClient();
    await kcAdminClient.auth(credentials);
    await kcAdminClient.realms.update(
      { realm: "master" },
      { organizationsEnabled: true },
    );
  });

  it("retrieves empty organizations list", async () => {
    const organizations = await kcAdminClient.organizations.find();
    expect(organizations).to.be.ok;
    expect(organizations).to.be.empty;
  });

  it("creates, updates, and removes an organization", async () => {
    const myOrganization = {
      name: "orga",
      enabled: true,
      domains: [
        {
          name: "orga.com",
        },
      ],
    };

    const org = await kcAdminClient.organizations.create(myOrganization);
    let allOrganizations = await kcAdminClient.organizations.find();
    expect(allOrganizations).to.be.ok;
    expect(allOrganizations).to.be.not.empty;

    myOrganization.enabled = false;
    await kcAdminClient.organizations.updateById(
      { id: org.id },
      myOrganization,
    );

    allOrganizations = await kcAdminClient.organizations.find();
    expect(allOrganizations).to.be.ok;
    expect(allOrganizations.length).to.equal(1);
    expect(allOrganizations[0].enabled).to.be.false;

    await kcAdminClient.organizations.delById({
      id: org.id,
    });
    allOrganizations = await kcAdminClient.organizations.find();
    expect(allOrganizations).to.be.ok;
    expect(allOrganizations).to.be.empty;
  });

  it("crud a group for organizations", async () => {
    const org = await kcAdminClient.organizations.create({
      name: "orgG",
      enabled: true,
      domains: [{ name: "orgg.com" }],
    });

    const group = await kcAdminClient.organizations.groups(org.id).create({
      name: "cool-group",
    });
    expect(group.id).to.be.ok;

    await kcAdminClient.organizations.groups(org.id).del({
      id: group.id!,
    });
  });

  it("manages organization roles", async () => {
    const suffix = faker.string.alphanumeric(8).toLowerCase();
    const names = {
      organization: `org-roles-${suffix}`,
      user: `organization-role-user-${suffix}`,
      childRole: `child-role-${suffix}`,
      organizationRole: `organization-role-${suffix}`,
      realmRole: `organization-composite-realm-role-${suffix}`,
      client: `organization-composite-client-${suffix}`,
      clientRole: `organization-composite-client-role-${suffix}`,
    };
    let organizationId: string | undefined;
    let userId: string | undefined;
    let organizationRoleId: string | undefined;
    let childRoleId: string | undefined;
    let realmRoleName: string | undefined;
    let clientId: string | undefined;
    let compositeRoles: { id?: string }[] = [];

    const ignoreCleanupError = async (cleanup: () => Promise<unknown>) => {
      try {
        await cleanup();
      } catch {
        // Cleanup is best-effort so it cannot mask the original test failure.
      }
    };

    try {
      const org = await kcAdminClient.organizations.create({
        name: names.organization,
        alias: names.organization,
        enabled: true,
        domains: [{ name: `${names.organization}.example.org` }],
      });
      organizationId = org.id;

      const user = await kcAdminClient.users.create({
        username: names.user,
        enabled: true,
      });
      userId = user.id;
      await kcAdminClient.organizations.addMember({
        orgId: organizationId,
        userId,
      });

      const defaultRole = await kcAdminClient.organizations.findDefaultRole({
        orgId: organizationId,
      });
      expect(defaultRole?.id).to.be.ok;

      const child = await kcAdminClient.organizations.createRole({
        orgId: organizationId,
        name: names.childRole,
      });
      childRoleId = child.id;
      const created = await kcAdminClient.organizations.createRole({
        orgId: organizationId,
        name: names.organizationRole,
        description: "before update",
      });
      organizationRoleId = created.id;

      expect(
        await kcAdminClient.organizations.countRoles({
          orgId: organizationId,
        }),
      ).to.equal(3);
      expect(
        await kcAdminClient.organizations.listRoles({
          orgId: organizationId,
          search: names.organizationRole,
          briefRepresentation: false,
        }),
      ).to.have.length(1);

      await kcAdminClient.organizations.updateRole(
        { orgId: organizationId, roleId: organizationRoleId },
        { name: names.organizationRole, description: "after update" },
      );
      const role = await kcAdminClient.organizations.findRole({
        orgId: organizationId,
        roleId: organizationRoleId,
      });
      expect(role?.description).to.equal("after update");

      await kcAdminClient.roles.create({ name: names.realmRole });
      realmRoleName = names.realmRole;
      const realmRole = await kcAdminClient.roles.findOneByName({
        name: names.realmRole,
      });
      const client = await kcAdminClient.clients.create({
        clientId: names.client,
      });
      clientId = client.id;
      await kcAdminClient.clients.createRole({
        id: clientId,
        name: names.clientRole,
      });
      const clientRole = await kcAdminClient.clients.findRole({
        id: clientId,
        roleName: names.clientRole,
      });

      expect(
        await kcAdminClient.organizations.listAvailableRoleComposites({
          orgId: organizationId,
          roleId: organizationRoleId,
          source: "realm",
          search: names.realmRole,
          first: 0,
          max: 1,
        }),
      ).to.have.length(1);

      compositeRoles = [
        { id: childRoleId },
        { id: realmRole!.id },
        { id: clientRole!.id },
      ];
      await kcAdminClient.organizations.addRoleComposites(
        { orgId: organizationId, roleId: organizationRoleId },
        compositeRoles,
      );
      expect(
        await kcAdminClient.organizations.listRoleComposites({
          orgId: organizationId,
          roleId: organizationRoleId,
        }),
      ).to.have.length(3);
      expect(
        await kcAdminClient.organizations.listEffectiveRoleComposites({
          orgId: organizationId,
          roleId: organizationRoleId,
          first: 0,
          max: 10,
        }),
      ).to.have.length(3);
      expect(
        await kcAdminClient.organizations.listRealmRoleComposites({
          orgId: organizationId,
          roleId: organizationRoleId,
        }),
      ).to.have.length(1);
      expect(
        await kcAdminClient.organizations.listClientRoleComposites({
          orgId: organizationId,
          roleId: organizationRoleId,
          clientId,
        }),
      ).to.have.length(1);

      await kcAdminClient.organizations.addRoleUsers(
        { orgId: organizationId, roleId: organizationRoleId },
        [{ id: userId }],
      );
      expect(
        await kcAdminClient.organizations.listRoleUsers({
          orgId: organizationId,
          roleId: organizationRoleId,
          briefRepresentation: true,
        }),
      ).to.have.length(1);
      await kcAdminClient.organizations.delRoleUsers(
        { orgId: organizationId, roleId: organizationRoleId },
        [{ id: userId }],
      );

      await kcAdminClient.organizations.delRoleComposites(
        { orgId: organizationId, roleId: organizationRoleId },
        compositeRoles,
      );
      await kcAdminClient.organizations.delRole({
        orgId: organizationId,
        roleId: organizationRoleId,
      });
      expect(
        await kcAdminClient.organizations.findRole({
          orgId: organizationId,
          roleId: organizationRoleId,
        }),
      ).to.be.null;
    } finally {
      const cleanupOrganizationId = organizationId;
      const cleanupOrganizationRoleId = organizationRoleId;
      const cleanupChildRoleId = childRoleId;
      const cleanupUserId = userId;
      const cleanupRealmRoleName = realmRoleName;
      const cleanupClientId = clientId;

      if (cleanupOrganizationId && cleanupOrganizationRoleId && cleanupUserId) {
        await ignoreCleanupError(() =>
          kcAdminClient.organizations.delRoleUsers(
            {
              orgId: cleanupOrganizationId,
              roleId: cleanupOrganizationRoleId,
            },
            [{ id: cleanupUserId }],
          ),
        );
      }
      if (
        cleanupOrganizationId &&
        cleanupOrganizationRoleId &&
        compositeRoles.length > 0
      ) {
        await ignoreCleanupError(() =>
          kcAdminClient.organizations.delRoleComposites(
            {
              orgId: cleanupOrganizationId,
              roleId: cleanupOrganizationRoleId,
            },
            compositeRoles,
          ),
        );
      }
      if (cleanupOrganizationId && cleanupOrganizationRoleId) {
        await ignoreCleanupError(() =>
          kcAdminClient.organizations.delRole({
            orgId: cleanupOrganizationId,
            roleId: cleanupOrganizationRoleId,
          }),
        );
      }
      if (cleanupOrganizationId && cleanupChildRoleId) {
        await ignoreCleanupError(() =>
          kcAdminClient.organizations.delRole({
            orgId: cleanupOrganizationId,
            roleId: cleanupChildRoleId,
          }),
        );
      }
      if (cleanupOrganizationId) {
        await ignoreCleanupError(() =>
          kcAdminClient.organizations.delById({ id: cleanupOrganizationId }),
        );
      }
      if (cleanupUserId) {
        await ignoreCleanupError(() =>
          kcAdminClient.users.del({ id: cleanupUserId }),
        );
      }
      if (cleanupRealmRoleName) {
        await ignoreCleanupError(() =>
          kcAdminClient.roles.delByName({ name: cleanupRealmRoleName }),
        );
      }
      if (cleanupClientId) {
        await ignoreCleanupError(() =>
          kcAdminClient.clients.del({ id: cleanupClientId }),
        );
      }
    }
  });
});
