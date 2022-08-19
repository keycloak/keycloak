import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type AttributeParams = {
  realm: string;
  attributeName: string;
};

export const AttributeRoute: RouteDef = {
  path: "/:realm/realm-settings/user-profile/attributes/:attributeName/edit-attribute",
  component: lazy(() => import("../NewAttributeSettings")),
  breadcrumb: (t) => t("realm-settings:editAttribute"),
  access: "manage-realm",
};

export const toAttribute = (params: AttributeParams): Partial<Path> => ({
  pathname: generatePath(AttributeRoute.path, params),
});
