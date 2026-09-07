import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { describe, expect, it } from "vitest";
import {
  canManageUiExtension,
  canViewUiExtension,
  getNavSection,
  getRequiredManageRoles,
  getRequiredViewRoles,
  getTabTitle,
} from "./uiExtensionAccess";

const page = (
  metadata: Record<string, unknown> = {},
): ComponentTypeRepresentation =>
  ({
    id: "my-extension",
    metadata,
  }) as ComponentTypeRepresentation;

const access = (roles: string[]) => ({
  hasAccess: (...types: string[]) =>
    types.every((type) => roles.includes(type)),
  hasSomeAccess: (...types: string[]) =>
    types.some((type) => roles.includes(type)),
});

describe("uiExtensionAccess", () => {
  it("uses realm roles by default", () => {
    expect(getRequiredViewRoles(page())).toEqual(["view-realm"]);
    expect(getRequiredManageRoles(page())).toEqual(["manage-realm"]);
  });

  it("reads declared roles from metadata", () => {
    const extension = page({
      requiredViewRoles: ["view-clients"],
      requiredManageRoles: ["manage-clients"],
    });

    expect(getRequiredViewRoles(extension)).toEqual(["view-clients"]);
    expect(getRequiredManageRoles(extension)).toEqual(["manage-clients"]);
  });

  it("gates view and manage access", () => {
    const extension = page({
      requiredViewRoles: ["view-clients"],
      requiredManageRoles: ["manage-clients"],
    });

    expect(canViewUiExtension(extension, access(["view-clients"]))).toBe(true);
    expect(canViewUiExtension(extension, access(["view-realm"]))).toBe(false);
    expect(canManageUiExtension(extension, access(["manage-clients"]))).toBe(
      true,
    );
    expect(canManageUiExtension(extension, access(["manage-realm"]))).toBe(
      false,
    );
  });

  it("resolves navigation sections", () => {
    expect(getNavSection(page())).toBe("configure");
    expect(getNavSection(page({ navSection: "manage" }))).toBe("manage");
    expect(getNavSection(page({ navSection: "extensions" }))).toBe(
      "extensions",
    );
  });

  it("resolves tab titles from metadata", () => {
    expect(getTabTitle(page(), (key) => `t:${key}`)).toBe("t:my-extension");
    expect(
      getTabTitle(page({ tabLabel: "customTab" }), (key) => `t:${key}`),
    ).toBe("t:customTab");
  });
});
