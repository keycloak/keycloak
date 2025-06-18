import type PasswordPolicyTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/passwordPolicyTypeRepresentation";
import { describe, expect, it } from "vitest";
import { parsePolicy, serializePolicy, SubmittedValues } from "./util";

describe("serializePolicy", () => {
  it("returns an empty string if there are no policies", () => {
    expect(serializePolicy([], {})).toEqual("");
  });

  it("encodes the policies", () => {
    const policies: PasswordPolicyTypeRepresentation[] = [
      { id: "one" },
      { id: "two" },
    ];

    const submittedValues: SubmittedValues = {
      one: "value1",
      two: "value2",
    };

    expect(serializePolicy(policies, submittedValues)).toEqual(
      "one(value1) and two(value2)",
    );
  });
});

describe("parsePolicy", () => {
  it("returns an empty array if an empty value is passed", () => {
    expect(parsePolicy("", [])).toEqual([]);
  });

  it("parses the policy", () => {
    const policies: PasswordPolicyTypeRepresentation[] = [
      { id: "one" },
      { id: "two" },
    ];

    expect(parsePolicy("one(value1) and two", policies)).toEqual([
      { id: "one", value: "value1" },
      { id: "two" },
    ]);
  });

  it("parses the policy and trims excessive whitespace", () => {
    const policies: PasswordPolicyTypeRepresentation[] = [
      { id: "one" },
      { id: "two" },
    ];

    expect(parsePolicy("one( value1 ) and  two ", policies)).toEqual([
      { id: "one", value: "value1" },
      { id: "two" },
    ]);
  });

  it("parses the policy and it handles unescaped values", () => {
    const policies: PasswordPolicyTypeRepresentation[] = [{ id: "one" }];

    expect(parsePolicy("one(value1", policies)).toEqual([{ id: "one" }]);
  });

  it("parses the policy and preserves nested parentheses", () => {
    const policies: PasswordPolicyTypeRepresentation[] = [{ id: "one" }];

    expect(parsePolicy("one(value1))", policies)).toEqual([
      { id: "one", value: "value1)" },
    ]);
  });

  it("parses the policy and preserves only existing entries", () => {
    const policies: PasswordPolicyTypeRepresentation[] = [{ id: "two" }];

    expect(parsePolicy("one(value1) and two", policies)).toEqual([
      { id: "two" },
    ]);
  });
});
