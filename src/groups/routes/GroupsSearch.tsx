import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { SearchGroups } from "../SearchGroups";

export type GroupsSearchParams = { realm: string };

export const GroupsSearchRoute: RouteDef = {
  path: "/:realm/groups/search",
  component: SearchGroups,
  breadcrumb: (t) => t("groups:searchGroups"),
  access: "query-groups",
};

export const toGroupsSearch = (
  params: GroupsSearchParams
): LocationDescriptorObject => ({
  pathname: generatePath(GroupsSearchRoute.path, params),
});
