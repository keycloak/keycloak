import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  ClientSsfTabRoutePath,
  type ClientSsfTabParams,
  type SsfClientTab,
  toSsfClientTab,
} from "./ClientSsfTab.routes";

export type { ClientSsfTabParams, SsfClientTab };
export { toSsfClientTab };

const ClientDetails = lazy(() => import("../ClientDetails"));

export const ClientSsfTabRoute: AppRouteObject = {
  path: ClientSsfTabRoutePath,
  element: <ClientDetails />,
  handle: {
    access: "view-clients",
    breadcrumb: (t) => t("clientSettings"),
  },
};
