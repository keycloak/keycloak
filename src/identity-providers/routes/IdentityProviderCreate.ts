import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { AddIdentityProvider } from "../add/AddIdentityProvider";

export type IdentityProviderCreateParams = {
  realm: string;
  providerId: string;
};

export const IdentityProviderCreateRoute: RouteDef = {
  path: "/:realm/identity-providers/:providerId/add",
  component: AddIdentityProvider,
  breadcrumb: (t) => t("identity-providers:addProvider"),
  access: "manage-identity-providers",
};

export const toIdentityProviderCreate = (
  params: IdentityProviderCreateParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProviderCreateRoute.path, params),
});
