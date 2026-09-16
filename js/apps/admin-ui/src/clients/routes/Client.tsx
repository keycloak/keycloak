import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  ClientRoutePath,
  type ClientParams,
  type ClientTab,
  toClient,
} from "./Client.routes";

export type { ClientParams, ClientTab };
export { toClient };

const ClientDetails = lazy(() => import("../ClientDetails"));

export const ClientRoute: AppRouteObject = {
  path: ClientRoutePath,
  element: <ClientDetails />,
  handle: {
    access: "query-clients",
    breadcrumb: (t) => t("clientSettings"),
  },
};
