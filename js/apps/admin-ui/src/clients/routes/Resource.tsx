import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type ResourceDetailsParams = {
  realm: string;
  id: string;
  resourceId?: string;
};

const ResourceDetails = lazy(() => import("../authorization/ResourceDetails"));

export const ResourceDetailsRoute: AppRouteObject = {
  path: "/:realm/clients/:id/authorization/resource",
  element: <ResourceDetails />,
  breadcrumb: (t) => t("resourceDetails"),
  handle: {
    access: "view-clients",
  },
};

export const ResourceDetailsWithResourceIdRoute: AppRouteObject = {
  ...ResourceDetailsRoute,
  path: "/:realm/clients/:id/authorization/resource/:resourceId",
};

export const toResourceDetails = (
  params: ResourceDetailsParams,
): Partial<Path> => {
  const path = params.resourceId
    ? ResourceDetailsWithResourceIdRoute.path
    : ResourceDetailsRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
