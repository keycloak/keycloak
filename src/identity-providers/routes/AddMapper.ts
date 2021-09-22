import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { AddMapper } from "../add/AddMapper";

export type IdentityProviderAddMapperParams = {
  realm: string;
  providerId: string;
  alias: string;
  tab: string;
};

export const IdentityProviderAddMapperRoute: RouteDef = {
  path: "/:realm/identity-providers/:providerId/:alias/:tab/create",
  component: AddMapper,
  access: "manage-identity-providers",
  breadcrumb: (t) => t("identity-providers:addIdPMapper"),
};

export const toIdentityProviderAddMapper = (
  params: IdentityProviderAddMapperParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProviderAddMapperRoute.path, params),
});
