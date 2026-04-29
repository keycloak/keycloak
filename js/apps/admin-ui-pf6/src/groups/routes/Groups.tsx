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

export const GroupsWithIdRoute: AppRouteObject = {
  ...GroupsRoute,
  path: "/:realm/groups/:id",
};

export const OrgGroupsWithIdRoute: AppRouteObject = {
  ...OrgGroupsRoute,
  path: "/:realm/organizations/:orgId/groups/:id",
};

export const toGroups = (params: GroupsParams): Partial<Path> => {
  const routes = {
    orgGroups: params.id ? OrgGroupsWithIdRoute.path : OrgGroupsRoute.path,
    realmGroups: params.id ? GroupsWithIdRoute.path : GroupsRoute.path,
  };

  const path = params.orgId ? routes.orgGroups : routes.realmGroups;

  return {
    pathname: generatePath(path, params),
  };
};
