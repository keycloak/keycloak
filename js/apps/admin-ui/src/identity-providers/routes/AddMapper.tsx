import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
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
  handle: {
    access: "manage-identity-providers",
    breadcrumb: (t) => t("addIdPMapper"),
  },
};

export const toIdentityProviderAddMapper = (
  params: IdentityProviderAddMapperParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderAddMapperRoute.path, params),
});
