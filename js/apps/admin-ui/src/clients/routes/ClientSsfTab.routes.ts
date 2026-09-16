import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

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

export const ClientSsfTabRoutePath = "/:realm/clients/:clientId/ssf/:tab";

export const toSsfClientTab = (params: ClientSsfTabParams): Partial<Path> => ({
  pathname: generateEncodedPath(ClientSsfTabRoutePath, params),
});
