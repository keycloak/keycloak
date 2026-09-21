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

  it("handles undefined domains with no serverDomains", () => {
    const form = {
      name: "test-org",
      alias: "test-org",
    } as OrganizationFormType;

    const result = convertToOrg(form);

    expect(result.domains).toBeUndefined();
  });

  it("passes through serverDomains when domains is undefined", () => {
    const form = {
      name: "test-org",
      alias: "test-org",
      serverDomains: [
        { name: "example.com", verified: true },
        { name: "acme.com", verified: false },
      ],
    } as OrganizationFormType;

    const result = convertToOrg(form);

    expect(result.domains).toEqual([
      { name: "example.com", verified: true },
      { name: "acme.com", verified: false },
    ]);
  });

  it("does not include serverDomains in the converted org", () => {
    const form = {
      name: "test-org",
      alias: "test-org",
      serverDomains: [{ name: "example.com", verified: true }],
    } as OrganizationFormType;

    const result = convertToOrg(form);

    expect(result).not.toHaveProperty("serverDomains");
  });
});
