import { describe, expect, it } from "vitest";
import { stripForeignAuthResponse } from "./stripForeignAuthResponse";

const CONSOLE_URL = "https://localhost:8443/realms/master/account/";

describe("stripForeignAuthResponse", () => {
  it("strips a response this application did not request", () => {
    // an organization invitation completes an authorization code flow that lands here
    const url = `${CONSOLE_URL}?session_state=abc&iss=https%3A%2F%2Flocalhost%3A8443%2Frealms%2Fmaster&code=def`;

    expect(stripForeignAuthResponse(url)).toBe(CONSOLE_URL);
  });

  it("strips an error response this application did not request", () => {
    const url = `${CONSOLE_URL}?error=access_denied&error_description=denied`;

    expect(stripForeignAuthResponse(url)).toBe(CONSOLE_URL);
  });

  it("keeps a response carrying state, which keycloak-js handles itself", () => {
    const url = `${CONSOLE_URL}?code=def&state=ghi&session_state=abc`;

    expect(stripForeignAuthResponse(url)).toBeUndefined();
  });

  it("keeps a URL without a response", () => {
    expect(
      stripForeignAuthResponse(`${CONSOLE_URL}#/account-security/signing-in`),
    ).toBeUndefined();
  });

  it("preserves the path, the fragment and unrelated parameters", () => {
    const url = `${CONSOLE_URL}groups?code=def&iss=abc&referrer=admin#/details`;

    expect(stripForeignAuthResponse(url)).toBe(
      `${CONSOLE_URL}groups?referrer=admin#/details`,
    );
  });
});
