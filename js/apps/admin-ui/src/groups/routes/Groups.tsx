import { lazy } from "react";
import { generatePath, type Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type GroupsParams = {
  realm: string;
  id?: string;
  lazy?: string;
  orgId?: string;
};

const GroupsSection = lazy(() => import("../GroupsSection"));

export const GroupsRoute: AppRouteObject = {
  path: "/:realm/groups/*",
  element: <GroupsSection />,
  handle: {
    access: "query-groups",
  },
};

export const OrgGroupsRoute: AppRouteObject = {
  path: "/:realm/organizations/:orgId/groups/*",
  element: <GroupsSection />,
  handle: {
    access: "query-groups",
  },
};

export const toGroups = (params: GroupsParams): Partial<Path> => {
  const path = params.orgId ? OrgGroupsRoute.path : GroupsRoute.path;

  return {
    pathname: generatePath(path, { ...params, "*": params.id }),
  };
};
