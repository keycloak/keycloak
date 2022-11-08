// tslint:disable:no-unused-expression
import * as chai from "chai";
import { KeycloakAdminClient } from "../src/client.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Sessions", () => {
  let client: KeycloakAdminClient;

  before(async () => {
    client = new KeycloakAdminClient();
    await client.auth(credentials);
  });

  it("list sessions", async () => {
    const sessions = await client.sessions.find();
    expect(sessions).to.be.ok;
    expect(sessions.length).to.be.eq(1);
    expect(sessions[0].clientId).to.be.eq("admin-cli");
  });
});
