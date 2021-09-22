import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { AddMapper } from "../add/AddMapper";

export type IdentityProviderEditMapperParams = {
  realm: string;
  providerId: string;
  alias: string;
  id: string;
};

export const IdentityProviderEditMapperRoute: RouteDef = {
  path: "/:realm/identity-providers/:providerId/:alias/mappers/:id",
  component: AddMapper,
  access: "manage-identity-providers",
  breadcrumb: (t) => t("identity-providers:editIdPMapper"),
};

export const toIdentityProviderEditMapper = (
  params: IdentityProviderEditMapperParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProviderEditMapperRoute.path, params),
});
