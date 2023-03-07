import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type IdentityProviderAddMapperParams = {
  realm: string;
  providerId: string;
  alias: string;
  tab: string;
};

export const IdentityProviderAddMapperRoute: RouteDef = {
  path: "/:realm/identity-providers/:providerId/:alias/:tab/create",
  component: lazy(() => import("../add/AddMapper")),
  access: "manage-identity-providers",
  breadcrumb: (t) => t("identity-providers:addIdPMapper"),
};

export const toIdentityProviderAddMapper = (
  params: IdentityProviderAddMapperParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProviderAddMapperRoute.path, params),
});
