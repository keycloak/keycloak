import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

/**
 * The five sub-tabs of the SSF view on a client. Mirrors the pattern
 * used by Client Scopes (setup / evaluate) so the SSF view's sub-tabs
 * are deep-linkable from URLs and bookmarkable per section.
 */
export type SsfClientTab =
  | "receiver"
  | "stream"
  | "subjects"
  | "event-search"
  | "emit-events";

export type ClientSsfTabParams = {
  realm: string;
  clientId: string;
  tab: SsfClientTab;
};

const ClientDetails = lazy(() => import("../ClientDetails"));

export const ClientSsfTabRoute: AppRouteObject = {
  path: "/:realm/clients/:clientId/ssf/:tab",
  element: <ClientDetails />,
  handle: {
    access: "view-clients",
    breadcrumb: (t) => t("clientSettings"),
  },
};

export const toSsfClientTab = (params: ClientSsfTabParams): Partial<Path> => ({
  pathname: generateEncodedPath(ClientSsfTabRoute.path, params),
});
