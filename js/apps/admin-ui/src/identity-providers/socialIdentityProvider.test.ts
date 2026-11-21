import { describe, expect, it } from "vitest";

import { isSocialIdentityProvider } from "./socialIdentityProvider";

describe("isSocialIdentityProvider", () => {
  const serverInfo = {
    componentTypes: {
      "org.keycloak.broker.social.SocialIdentityProvider": [
        { id: "google" },
        { id: "microsoft" },
      ],
    },
  };

  it("detects social providers from server info", () => {
    expect(isSocialIdentityProvider("google", serverInfo)).toBe(true);
    expect(isSocialIdentityProvider("oidc", serverInfo)).toBe(false);
    expect(isSocialIdentityProvider(undefined, serverInfo)).toBe(false);
  });
});
