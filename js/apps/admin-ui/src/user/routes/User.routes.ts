import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath.js";

export type UserTab =
  | "settings"
  | "groups"
  | "organizations"
  | "consents"
  | "attributes"
  | "sessions"
  | "credentials"
  | "role-mapping"
  | "identity-provider-links"
  | "events"
  | "workflows"
  | "verifiable-credentials";

export type UserParams = {
  realm: string;
  id: string;
  tab: UserTab;
};

export const UserRoutePath = "/:realm/users/:id/:tab";

export const toUser = (params: UserParams): Partial<Path> => ({
  pathname: generateEncodedPath(UserRoutePath, params),
});
