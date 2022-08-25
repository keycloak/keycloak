import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type GroupsSearchParams = { realm: string };

export const GroupsSearchRoute: RouteDef = {
  path: "/:realm/groups/search",
  component: lazy(() => import("../SearchGroups")),
  breadcrumb: (t) => t("groups:searchGroups"),
  access: "query-groups",
};

export const toGroupsSearch = (params: GroupsSearchParams): Partial<Path> => ({
  pathname: generatePath(GroupsSearchRoute.path, params),
});
