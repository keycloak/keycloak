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
});
