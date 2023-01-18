import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type IdentityProviderEditMapperParams = {
  realm: string;
  providerId: string;
  alias: string;
  id: string;
};

export const IdentityProviderEditMapperRoute: RouteDef = {
  path: "/:realm/identity-providers/:providerId/:alias/mappers/:id",
  component: lazy(() => import("../add/AddMapper")),
  access: "manage-identity-providers",
  breadcrumb: (t) => t("identity-providers:editIdPMapper"),
};

export const toIdentityProviderEditMapper = (
  params: IdentityProviderEditMapperParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProviderEditMapperRoute.path, params),
});
