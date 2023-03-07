import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type NewAttributesGroupParams = {
  realm: string;
};

export const NewAttributesGroupRoute: RouteDef = {
  path: "/:realm/realm-settings/user-profile/attributesGroup/new",
  component: lazy(() => import("../user-profile/AttributesGroupDetails")),
  breadcrumb: (t) => t("realm-settings:createGroupText"),
  access: "view-realm",
};

export const toNewAttributesGroup = (
  params: NewAttributesGroupParams
): Partial<Path> => ({
  pathname: generatePath(NewAttributesGroupRoute.path, params),
});
