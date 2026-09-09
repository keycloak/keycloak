import { describe, expect, it } from "vitest";
import { identityProviderRedirectUrl } from "./identity-provider-redirect-url";

describe("identityProviderRedirectUrl", () => {
  it("uses serverBaseUrl when the realm has no frontendUrl", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com",
    );

    expect(result).toBe(
      "https://sso.example.com/realms/my-realm/broker/oidc/endpoint",
    );
  });

  it("uses the realm frontendUrl when it is an absolute URL", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com",
      "https://auth.tenant.example.com",
    );

    expect(result).toBe(
      "https://auth.tenant.example.com/realms/my-realm/broker/oidc/endpoint",
    );
  });

  it("falls back to serverBaseUrl when frontendUrl is blank", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com",
      "",
    );

    expect(result).toBe(
      "https://sso.example.com/realms/my-realm/broker/oidc/endpoint",
    );
  });

  it("ignores a non-absolute frontendUrl", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com",
      "not-a-url",
    );

    expect(result).toBe(
      "https://sso.example.com/realms/my-realm/broker/oidc/endpoint",
    );
  });

  it("does not produce a double slash when serverBaseUrl has a trailing slash", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com/",
    );

    expect(result).toBe(
      "https://sso.example.com/realms/my-realm/broker/oidc/endpoint",
    );
  });

  it("uses (and preserves) an uppercase scheme the server accepts", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com",
      "HTTPS://auth.tenant.example.com",
    );

    expect(result).toBe(
      "HTTPS://auth.tenant.example.com/realms/my-realm/broker/oidc/endpoint",
    );
  });

  it("preserves dot segments instead of canonicalizing them", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com",
      "https://auth.tenant.example.com/a/../public",
    );

    expect(result).toBe(
      "https://auth.tenant.example.com/a/../public/realms/my-realm/broker/oidc/endpoint",
    );
  });

  it("falls back when frontendUrl looks like a URL prefix but does not parse", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com",
      "http-not-a-url",
    );

    expect(result).toBe(
      "https://sso.example.com/realms/my-realm/broker/oidc/endpoint",
    );
  });

  it("falls back when frontendUrl uses a non-http scheme", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com",
      "ftp://auth.tenant.example.com",
    );

    expect(result).toBe(
      "https://sso.example.com/realms/my-realm/broker/oidc/endpoint",
    );
  });

  it("keeps a query/fragment on frontendUrl after the appended route", () => {
    const result = identityProviderRedirectUrl(
      "oidc",
      "my-realm",
      "https://sso.example.com",
      "https://auth.tenant.example.com/base?x=1",
    );

    expect(result).toBe(
      "https://auth.tenant.example.com/base/realms/my-realm/broker/oidc/endpoint?x=1",
    );
  });
});
