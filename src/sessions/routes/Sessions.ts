import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { SessionsSection } from "../SessionsSection";

export type SessionsParams = { realm: string };

export const SessionsRoute: RouteDef = {
  path: "/:realm/sessions",
  component: SessionsSection,
  breadcrumb: (t) => t("sessions:title"),
  access: "view-realm",
};

export const toSessions = (
  params: SessionsParams
): LocationDescriptorObject => ({
  pathname: generatePath(SessionsRoute.path, params),
});
