// tslint:disable:no-unused-expression
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Who am I", () => {
  let client: KeycloakAdminClient;

  before(async () => {
    client = new KeycloakAdminClient();
    await client.auth(credentials);
  });

  it("list who I am", async () => {
    const whoAmI = await client.whoAmI.find();
    expect(whoAmI).to.be.ok;
    expect(whoAmI.displayName).to.be.equal("admin");
  });
});
