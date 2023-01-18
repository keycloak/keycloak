import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type SessionsParams = { realm: string };

export const SessionsRoute: RouteDef = {
  path: "/:realm/sessions",
  component: lazy(() => import("../SessionsSection")),
  breadcrumb: (t) => t("sessions:title"),
  access: ["view-realm", "view-clients", "view-users"],
};

export const toSessions = (params: SessionsParams): Partial<Path> => ({
  pathname: generatePath(SessionsRoute.path, params),
});
