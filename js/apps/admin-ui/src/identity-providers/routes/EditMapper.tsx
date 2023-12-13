import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderEditMapperParams = {
  realm: string;
  providerId: string;
  alias: string;
  id: string;
};

const AddMapper = lazy(() => import("../add/AddMapper"));

export const IdentityProviderEditMapperRoute: AppRouteObject = {
  path: "/:realm/identity-providers/:providerId/:alias/mappers/:id",
  element: <AddMapper />,
  breadcrumb: (t) => t("editIdPMapper"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityProviderEditMapper = (
  params: IdentityProviderEditMapperParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderEditMapperRoute.path, params),
});
