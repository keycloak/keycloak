import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type NewResourceParams = { realm: string; id: string };

const ResourceDetails = lazy(() => import("../authorization/ResourceDetails"));

export const NewResourceRoute: AppRouteObject = {
  path: "/:realm/clients/:id/authorization/resource/new",
  element: <ResourceDetails />,
  breadcrumb: (t) => t("createResource"),
  handle: {
    access: (accessChecker) =>
      accessChecker.hasAny("manage-clients", "manage-authorization"),
  },
};

export const toCreateResource = (params: NewResourceParams): Partial<Path> => ({
  pathname: generateEncodedPath(NewResourceRoute.path, params),
});
