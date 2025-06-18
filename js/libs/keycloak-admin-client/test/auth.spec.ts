import * as chai from "chai";
import { getToken } from "../src/utils/auth.js";
import { credentials } from "./constants.js";

const expect = chai.expect;

describe("Authorization", () => {
  it("should get token from local keycloak", async () => {
    const data = await getToken({
      credentials,
    });

    expect(data).to.have.all.keys(
      "accessToken",
      "expiresIn",
      "refreshExpiresIn",
      "refreshToken",
      "tokenType",
      "notBeforePolicy",
      "sessionState",
      "scope",
    );
  });

  it("should get token from local keycloak with custom scope", async () => {
    const data = await getToken({
      credentials: {
        ...credentials,
        scopes: ["openid", "profile"],
      },
    });

    expect(data).to.have.all.keys(
      "accessToken",
      "expiresIn",
      "refreshExpiresIn",
      "refreshToken",
      "tokenType",
      "notBeforePolicy",
      "sessionState",
      "scope",
      "idToken",
    );

    expect(data.scope).to.equal("openid email profile");
  });
});
