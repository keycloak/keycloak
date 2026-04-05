import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
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
    access: (accessChecker) =>
      accessChecker.hasAny(
        "manage-clients",
        "view-authorization",
        "manage-authorization",
      ),
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
    pathname: generateEncodedPath(path, params),
  };
};
