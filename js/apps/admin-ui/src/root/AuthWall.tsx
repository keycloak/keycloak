import { AccessType } from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import { useMatches } from "react-router-dom";

import { ForbiddenSection } from "../ForbiddenSection";
import { useAccess } from "../context/access/Access";

function hasProp<K extends PropertyKey>(
  data: object,
  prop: K,
): data is Record<K, unknown> {
  return prop in data;
}

export const AuthWall = ({ children }: any) => {
  const matches = useMatches();
  const { hasAccess } = useAccess();

  const permissionNeeded = matches.flatMap(({ handle }) => {
    if (
      typeof handle !== "object" ||
      handle === null ||
      !hasProp(handle, "access")
    ) {
      return [];
    }

    if (Array.isArray(handle.access)) {
      return handle.access as AccessType[];
    }

    return [handle.access] as AccessType[];
  });

  return hasAccess(...permissionNeeded) ? (
    children
  ) : (
    <ForbiddenSection permissionNeeded={permissionNeeded} />
  );
};
