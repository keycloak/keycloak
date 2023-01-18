import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ResourceDetailsParams = {
  realm: string;
  id: string;
  resourceId?: string;
};

export const ResourceDetailsRoute: RouteDef = {
  path: "/:realm/clients/:id/authorization/resource",
  component: lazy(() => import("../authorization/ResourceDetails")),
  breadcrumb: (t) => t("clients:createResource"),
  access: "view-clients",
};

export const ResourceDetailsWithResourceIdRoute: RouteDef = {
  ...ResourceDetailsRoute,
  path: "/:realm/clients/:id/authorization/resource/:resourceId",
};

export const toResourceDetails = (
  params: ResourceDetailsParams
): Partial<Path> => {
  const path = params.resourceId
    ? ResourceDetailsWithResourceIdRoute.path
    : ResourceDetailsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
