import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { AddSamlConnect } from "../add/AddSamlConnect";

export type IdentityProviderSamlParams = { realm: string };

export const IdentityProviderSamlRoute: RouteDef = {
  path: "/:realm/identity-providers/saml",
  component: AddSamlConnect,
  breadcrumb: (t) => t("identity-providers:addSamlProvider"),
  access: "manage-identity-providers",
};

export const toIdentityProviderSaml = (
  params: IdentityProviderSamlParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProviderSamlRoute.path, params),
});
