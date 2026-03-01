import { ReactElement } from "react";
import { Link } from "react-router-dom";
import { Tooltip } from "@patternfly/react-core";

import type AdminEventRepresentation from "@keycloak/keycloak-admin-client/lib/defs/adminEventRepresentation";
import { useRealm } from "../context/realm-context/RealmContext";
import { toClient } from "../clients/routes/Client";
import { toGroups } from "../groups/routes/Groups";
import { toClientScope } from "../client-scopes/routes/ClientScope";
import { toUser } from "../user/routes/User";
import { toRealmRole } from "../realm-roles/routes/RealmRole";
import { toFlow } from "../authentication/routes/Flow";
import { toEditOrganization } from "../organizations/routes/EditOrganization";

type ResourceLinkProps = {
  event: AdminEventRepresentation;
};

const MAX_TEXT_LENGTH = 38;
const Truncate = ({
  text,
  children,
}: {
  text?: string;
  children: (text: string) => ReactElement;
}) => {
  const definedText = text || "";
  const needsTruncation = definedText.length > MAX_TEXT_LENGTH;
  const truncatedText = definedText.substring(0, MAX_TEXT_LENGTH);
  return needsTruncation ? (
    <Tooltip content={text}>{children(truncatedText + "…")}</Tooltip>
  ) : (
    children(definedText)
  );
};

const isLinkable = (event: AdminEventRepresentation) => {
  if (event.operationType === "DELETE") {
    return false;
  }
  return (
    event.resourceType === "USER" ||
    event.resourceType === "GROUP_MEMBERSHIP" ||
    event.resourceType === "GROUP" ||
    event.resourceType === "CLIENT" ||
    event.resourceType === "ORGANIZATION" ||
    event.resourceType === "ORGANIZATION_MEMBERSHIP" ||
    event.resourceType?.startsWith("AUTHORIZATION_RESOURCE") ||
    event.resourceType === "CLIENT_SCOPE" ||
    event.resourceType === "AUTH_FLOW" ||
    event.resourcePath?.startsWith("roles-by-id")
  );
};

const idRegex = new RegExp(
  /([0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12})/,
);

const createLink = (realm: string, event: AdminEventRepresentation) => {
  const part = idRegex.exec(event.resourcePath!);
  if (!part) {
    console.warn("event didn't contain a valid link", event);
    return "";
  }
  const id = part[1];

  if (
    event.resourceType === "CLIENT" ||
    event.resourceType?.startsWith("AUTHORIZATION_RESOURCE")
  ) {
    return toClient({
      realm,
      clientId: id,
      tab: event.resourceType === "CLIENT" ? "settings" : "authorization",
    });
  }

  if (event.resourceType === "GROUP") {
    return toGroups({ realm, id });
  }

  if (event.resourceType === "CLIENT_SCOPE") {
    return toClientScope({ realm, id, tab: "settings" });
  }

  if (
    event.resourceType === "USER" ||
    event.resourceType === "GROUP_MEMBERSHIP"
  ) {
    return toUser({ realm, id, tab: "settings" });
  }

  if (event.resourceType === "AUTH_FLOW") {
    return toFlow({ realm, id, usedBy: "-" });
  }

  if (event.resourcePath?.startsWith("roles-by-id")) {
    return toRealmRole({ realm, id, tab: "details" });
  }

  if (event.resourceType === "ORGANIZATION") {
    return toEditOrganization({ realm, id, tab: "settings" });
  }

  if (event.resourceType === "ORGANIZATION_MEMBERSHIP") {
    return toEditOrganization({ realm, id, tab: "members" });
  }

  return "";
};

export const ResourceLink = ({ event }: ResourceLinkProps) => {
  const { realm } = useRealm();
  return (
    <Truncate text={event.resourcePath}>
      {(text) =>
        isLinkable(event) ? (
          <Link to={createLink(realm, event)}>{text}</Link>
        ) : (
          <span>{text}</span>
        )
      }
    </Truncate>
  );
};

export const CellResourceLinkRenderer = (
  adminEvent: AdminEventRepresentation,
) => <ResourceLink event={adminEvent} />;
