import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type EventsTab = "user-events" | "admin-events" | "hooks";
export type EventHooksSubTab = "targets" | "logs";

export type EventsParams = {
  realm: string;
  tab?: EventsTab;
  subTab?: EventHooksSubTab;
};

const EventsSection = lazy(() => import("../EventsSection"));

export const EventsRoute: AppRouteObject = {
  path: "/:realm/events",
  element: <EventsSection />,
  handle: {
    access: "view-events",
    breadcrumb: (t) => t("titleEvents"),
  },
};

export const EventsRouteWithTab: AppRouteObject = {
  ...EventsRoute,
  path: "/:realm/events/:tab",
};

export const EventsRouteWithSubTab: AppRouteObject = {
  ...EventsRoute,
  path: "/:realm/events/:tab/:subTab",
};

export const toEvents = (params: EventsParams): Partial<Path> => {
  const path = params.subTab
    ? EventsRouteWithSubTab.path
    : params.tab
      ? EventsRouteWithTab.path
      : EventsRoute.path;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
