import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type NewAttributesGroupParams = {
  realm: string;
};

const AttributesGroupDetails = lazy(
  () => import("../user-profile/AttributesGroupDetails"),
);

export const NewAttributesGroupRoute: AppRouteObject = {
  path: "/:realm/realm-settings/user-profile/attributesGroup/new",
  element: <AttributesGroupDetails />,
  breadcrumb: (t) => t("createGroupText"),
  handle: {
    access: "view-realm",
  },
};

export const toNewAttributesGroup = (
  params: NewAttributesGroupParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(NewAttributesGroupRoute.path, params),
});
