// tslint:disable:no-unused-expression
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
    const org = await kcAdminClient.organizations.create({
      name: "orgRoles",
      alias: "org-roles",
      enabled: true,
      domains: [{ name: "roles.org" }],
    });
    const user = await kcAdminClient.users.create({
      username: "organization-role-user",
      enabled: true,
    });
    await kcAdminClient.organizations.addMember({
      orgId: org.id,
      userId: user.id,
    });

    const defaultRole = await kcAdminClient.organizations.findDefaultRole({
      orgId: org.id,
    });
    expect(defaultRole?.id).to.be.ok;

    const child = await kcAdminClient.organizations.createRole({
      orgId: org.id,
      name: "child-role",
    });
    const created = await kcAdminClient.organizations.createRole({
      orgId: org.id,
      name: "organization-role",
      description: "before update",
    });

    expect(
      await kcAdminClient.organizations.countRoles({ orgId: org.id }),
    ).to.equal(3);
    expect(
      await kcAdminClient.organizations.listRoles({
        orgId: org.id,
        search: "organization-role",
        briefRepresentation: false,
      }),
    ).to.have.length(1);

    await kcAdminClient.organizations.updateRole(
      { orgId: org.id, roleId: created.id },
      { name: "organization-role", description: "after update" },
    );
    const role = await kcAdminClient.organizations.findRole({
      orgId: org.id,
      roleId: created.id,
    });
    expect(role?.description).to.equal("after update");

    await kcAdminClient.roles.create({
      name: "organization-composite-realm-role",
    });
    const realmRole = await kcAdminClient.roles.findOneByName({
      name: "organization-composite-realm-role",
    });
    const client = await kcAdminClient.clients.create({
      clientId: "organization-composite-client",
    });
    await kcAdminClient.clients.createRole({
      id: client.id,
      name: "organization-composite-client-role",
    });
    const clientRole = await kcAdminClient.clients.findRole({
      id: client.id,
      roleName: "organization-composite-client-role",
    });

    await kcAdminClient.organizations.addRoleComposites(
      { orgId: org.id, roleId: created.id },
      [{ id: child.id }, { id: realmRole!.id }, { id: clientRole!.id }],
    );
    expect(
      await kcAdminClient.organizations.listRoleComposites({
        orgId: org.id,
        roleId: created.id,
      }),
    ).to.have.length(3);
    expect(
      await kcAdminClient.organizations.listRealmRoleComposites({
        orgId: org.id,
        roleId: created.id,
      }),
    ).to.have.length(1);
    expect(
      await kcAdminClient.organizations.listClientRoleComposites({
        orgId: org.id,
        roleId: created.id,
        clientId: client.id,
      }),
    ).to.have.length(1);

    await kcAdminClient.organizations.addRoleUsers(
      { orgId: org.id, roleId: created.id },
      [{ id: user.id }],
    );
    expect(
      await kcAdminClient.organizations.listRoleUsers({
        orgId: org.id,
        roleId: created.id,
        briefRepresentation: true,
      }),
    ).to.have.length(1);
    await kcAdminClient.organizations.delRoleUsers(
      { orgId: org.id, roleId: created.id },
      [{ id: user.id }],
    );

    await kcAdminClient.organizations.delRoleComposites(
      { orgId: org.id, roleId: created.id },
      [{ id: child.id }, { id: realmRole!.id }, { id: clientRole!.id }],
    );
    await kcAdminClient.organizations.delRole({
      orgId: org.id,
      roleId: created.id,
    });
    expect(
      await kcAdminClient.organizations.findRole({
        orgId: org.id,
        roleId: created.id,
      }),
    ).to.be.null;

    await kcAdminClient.organizations.delById({ id: org.id });
    await kcAdminClient.users.del({ id: user.id });
    await kcAdminClient.roles.delByName({
      name: "organization-composite-realm-role",
    });
    await kcAdminClient.clients.del({ id: client.id });
  });
});
