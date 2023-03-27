import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderAddMapperParams = {
  realm: string;
  providerId: string;
  alias: string;
  tab: string;
};

const AddMapper = lazy(() => import("../add/AddMapper"));

export const IdentityProviderAddMapperRoute: AppRouteObject = {
  path: "/:realm/identity-providers/:providerId/:alias/:tab/create",
  element: <AddMapper />,
  breadcrumb: (t) => t("identity-providers:addIdPMapper"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityProviderAddMapper = (
  params: IdentityProviderAddMapperParams
): Partial<Path> => ({
  pathname: generatePath(IdentityProviderAddMapperRoute.path, params),
});
