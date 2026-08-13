import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type AddAttributeParams = {
  realm: string;
};

const NewAttributeSettings = lazy(() => import("../NewAttributeSettings"));

export const AddAttributeRoute: AppRouteObject = {
  path: "/:realm/realm-settings/user-profile/attributes/add-attribute",
  element: <NewAttributeSettings />,
  handle: {
    access: "manage-realm",
    breadcrumb: (t) => t("createAttribute"),
  },
};

export const toAddAttribute = (params: AddAttributeParams): Partial<Path> => ({
  pathname: generateEncodedPath(AddAttributeRoute.path, params),
});
