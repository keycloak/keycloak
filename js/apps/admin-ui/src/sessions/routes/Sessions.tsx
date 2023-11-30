import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type SessionsParams = { realm: string };

const SessionsSection = lazy(() => import("../SessionsSection"));

export const SessionsRoute: AppRouteObject = {
  path: "/:realm/sessions",
  element: <SessionsSection />,
  breadcrumb: (t) => t("titleSessions"),
  handle: {
    access: ["view-realm", "view-clients", "view-users"],
  },
};

export const toSessions = (params: SessionsParams): Partial<Path> => ({
  pathname: generateEncodedPath(SessionsRoute.path, params),
});
