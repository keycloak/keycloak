import type { AccessType } from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";

type AccessChecker = {
  hasAccess: (...types: AccessType[]) => boolean;
  hasSomeAccess: (...types: AccessType[]) => boolean;
};

const DEFAULT_VIEW_ROLES: AccessType[] = ["view-realm"];
const DEFAULT_MANAGE_ROLES: AccessType[] = ["manage-realm"];

export type NavSection = "manage" | "configure" | "extensions";

export function getRequiredViewRoles(
  page: ComponentTypeRepresentation,
): AccessType[] {
  const roles = page.metadata.requiredViewRoles as string[] | undefined;
  return roles?.length ? (roles as AccessType[]) : DEFAULT_VIEW_ROLES;
}

export function getRequiredManageRoles(
  page: ComponentTypeRepresentation,
): AccessType[] {
  const roles = page.metadata.requiredManageRoles as string[] | undefined;
  return roles?.length ? (roles as AccessType[]) : DEFAULT_MANAGE_ROLES;
}

export function canViewUiExtension(
  page: ComponentTypeRepresentation,
  { hasSomeAccess }: AccessChecker,
): boolean {
  return hasSomeAccess(...getRequiredViewRoles(page));
}

export function canManageUiExtension(
  page: ComponentTypeRepresentation,
  { hasAccess }: AccessChecker,
): boolean {
  return hasAccess(...getRequiredManageRoles(page));
}

export function getNavSection(page: ComponentTypeRepresentation): NavSection {
  const section = page.metadata.navSection as NavSection | undefined;
  if (section === "manage" || section === "extensions") {
    return section;
  }
  return "configure";
}

export function getTabTitle(
  page: ComponentTypeRepresentation,
  t: (key: string) => string,
): string {
  const tabLabel = page.metadata.tabLabel as string | undefined;
  return tabLabel ? t(tabLabel) : t(page.id!);
}
