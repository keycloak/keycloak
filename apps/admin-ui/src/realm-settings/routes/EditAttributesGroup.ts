import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type EditAttributesGroupParams = {
  realm: string;
  name: string;
};

export const EditAttributesGroupRoute: RouteDef = {
  path: "/:realm/realm-settings/user-profile/attributesGroup/edit/:name",
  component: lazy(() => import("../user-profile/AttributesGroupDetails")),
  breadcrumb: (t) => t("realm-settings:editGroupText"),
  access: "view-realm",
};

export const toEditAttributesGroup = (
  params: EditAttributesGroupParams
): Partial<Path> => ({
  pathname: generatePath(EditAttributesGroupRoute.path, params),
});
