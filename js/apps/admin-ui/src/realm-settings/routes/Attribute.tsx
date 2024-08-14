import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type AttributeParams = {
  realm: string;
  attributeName: string;
};

const NewAttributeSettings = lazy(() => import("../NewAttributeSettings"));

export const AttributeRoute: AppRouteObject = {
  path: "/:realm/realm-settings/user-profile/attributes/:attributeName/edit-attribute",
  element: <NewAttributeSettings />,
  breadcrumb: (t) => t("editAttribute"),
  handle: {
    access: "manage-realm",
  },
};

export const toAttribute = (params: AttributeParams): Partial<Path> => ({
  pathname: generateEncodedPath(AttributeRoute.path, params),
});
