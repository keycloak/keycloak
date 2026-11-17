import { describe, expect, it } from "vitest";
import { toGroups } from "../routes/Groups";

describe("toGroups", () => {
  it("preserves hierarchy separators in realm group paths", () => {
    expect(
      toGroups({ realm: "master", id: "parent-id/child-id" }).pathname,
    ).toBe("/master/groups/parent-id/child-id");
  });

  it("preserves hierarchy separators in organization group paths", () => {
    expect(
      toGroups({
        realm: "master",
        orgId: "organization-id",
        id: "parent-id/child-id",
      }).pathname,
    ).toBe("/master/organizations/organization-id/groups/parent-id/child-id");
  });

  it("generates the group root path without an id", () => {
    expect(toGroups({ realm: "master" }).pathname).toBe("/master/groups");
  });
});
