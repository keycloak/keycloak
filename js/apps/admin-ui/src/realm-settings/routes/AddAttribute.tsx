import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateUnencodedPath } from "../../util";
import type { AppRouteObject } from "../../routes";

export type AddAttributeParams = {
  realm: string;
};

const NewAttributeSettings = lazy(() => import("../NewAttributeSettings"));

export const AddAttributeRoute: AppRouteObject = {
  path: "/:realm/realm-settings/user-profile/attributes/add-attribute",
  element: <NewAttributeSettings />,
  breadcrumb: (t) => t("createAttribute"),
  handle: {
    access: "manage-realm",
  },
};

export const toAddAttribute = (params: AddAttributeParams): Partial<Path> => ({
  pathname: generateUnencodedPath(AddAttributeRoute.path, params),
});
