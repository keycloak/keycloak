import { describe, expect, it } from "vitest";
import { convertToOrg, OrganizationFormType } from "./OrganizationForm";

describe("convertToOrg", () => {
  it("omits blank domain entries", () => {
    const form = {
      name: "test-org",
      alias: "test-org",
      domains: [""],
    } as OrganizationFormType;

    const result = convertToOrg(form);

    expect(result.domains).toEqual([]);
  });

  it("keeps valid domains and drops blanks mixed in", () => {
    const form = {
      name: "test-org",
      alias: "test-org",
      domains: ["example.com", "", "acme.com"],
    } as OrganizationFormType;

    const result = convertToOrg(form);

    expect(result.domains).toEqual([
      { name: "example.com", verified: false },
      { name: "acme.com", verified: false },
    ]);
  });

  it("trims whitespace from domain names", () => {
    const form = {
      name: "test-org",
      alias: "test-org",
      domains: [" example.com ", "  ", "acme.com"],
    } as OrganizationFormType;

    const result = convertToOrg(form);

    expect(result.domains).toEqual([
      { name: "example.com", verified: false },
      { name: "acme.com", verified: false },
    ]);
  });

  it("handles undefined domains", () => {
    const form = {
      name: "test-org",
      alias: "test-org",
    } as OrganizationFormType;

    const result = convertToOrg(form);

    expect(result.domains).toBeUndefined();
  });
});
