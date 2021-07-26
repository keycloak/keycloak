import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { IdentityProvidersSection } from "../IdentityProvidersSection";

export type IdentityProvidersParams = { realm: string };

export const IdentityProvidersRoute: RouteDef = {
  path: "/:realm/identity-providers",
  component: IdentityProvidersSection,
  breadcrumb: (t) => t("identityProviders"),
  access: "view-identity-providers",
};

export const toIdentityProviders = (
  params: IdentityProvidersParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProvidersRoute.path, params),
});
