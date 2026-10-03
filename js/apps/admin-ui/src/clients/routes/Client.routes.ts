import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

export type ClientTab =
  | "settings"
  | "keys"
  | "credentials"
  | "roles"
  | "clientScopes"
  | "advanced"
  | "mappers"
  | "authorization"
  | "serviceAccount"
  | "permissions"
  | "sessions"
  | "events"
  | "ssf";

export type ClientParams = {
  realm: string;
  clientId: string;
  tab: ClientTab;
};

export const ClientRoutePath = "/:realm/clients/:clientId/:tab";

export const toClient = (params: ClientParams): Partial<Path> => ({
  pathname: generateEncodedPath(ClientRoutePath, params),
});
