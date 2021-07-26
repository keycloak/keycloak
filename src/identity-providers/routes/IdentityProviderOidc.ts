import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { AddOpenIdConnect } from "../add/AddOpenIdConnect";

export type IdentityProviderOidcParams = { realm: string };

export const IdentityProviderOidcRoute: RouteDef = {
  path: "/:realm/identity-providers/oidc",
  component: AddOpenIdConnect,
  breadcrumb: (t) => t("identity-providers:addOpenIdProvider"),
  access: "manage-identity-providers",
};

export const toIdentityProviderOidc = (
  params: IdentityProviderOidcParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProviderOidcRoute.path, params),
});
