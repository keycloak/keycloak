import { describe, expect, it } from "vitest";
import {
  decodeBase32,
  decodeExecutionProviderId,
  inferSubflowProviderId,
} from "../execution-provider";

describe("execution-provider", () => {
  describe("decodeBase32", () => {
    it("decodes Keycloak-encoded script provider IDs", () => {
      // Base32.encode("script-test.js".getBytes()) from Keycloak's Base32
      expect(decodeBase32("ONRXE2LQOQWXIZLTOQXGU4Y")).toBe("script-test.js");
    });
  });

  describe("decodeExecutionProviderId", () => {
    it("returns script providers unchanged", () => {
      expect(decodeExecutionProviderId("script-my-auth.js")).toBe(
        "script-my-auth.js",
      );
    });

    it("decodes Base32-encoded script providers", () => {
      expect(decodeExecutionProviderId("ONRXE2LQOQWXIZLTOQXGU4Y")).toBe(
        "script-test.js",
      );
    });

    it("leaves ordinary authenticator IDs unchanged", () => {
      expect(decodeExecutionProviderId("auth-cookie")).toBe("auth-cookie");
      expect(decodeExecutionProviderId("identity-provider-redirector")).toBe(
        "identity-provider-redirector",
      );
    });
  });

  describe("inferSubflowProviderId", () => {
    it("detects basic-flow from CONDITIONAL choice", () => {
      expect(
        inferSubflowProviderId({
          authenticationFlow: true,
          requirementChoices: [
            "REQUIRED",
            "ALTERNATIVE",
            "DISABLED",
            "CONDITIONAL",
          ],
        }),
      ).toBe("basic-flow");
    });

    it("detects form-flow from REQUIRED/DISABLED only", () => {
      expect(
        inferSubflowProviderId({
          authenticationFlow: true,
          requirementChoices: ["REQUIRED", "DISABLED"],
        }),
      ).toBe("form-flow");
    });

    it("detects client-flow from ALTERNATIVE without CONDITIONAL", () => {
      expect(
        inferSubflowProviderId({
          authenticationFlow: true,
          requirementChoices: ["ALTERNATIVE", "REQUIRED", "DISABLED"],
        }),
      ).toBe("client-flow");
    });

    it("returns undefined for non-flow executions", () => {
      expect(
        inferSubflowProviderId({
          authenticationFlow: false,
          requirementChoices: ["REQUIRED", "DISABLED"],
        }),
      ).toBeUndefined();
    });
  });
});
