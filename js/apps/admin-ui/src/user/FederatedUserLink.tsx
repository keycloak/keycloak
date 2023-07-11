import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { Button } from "@patternfly/react-core";
import { useState } from "react";
import { Link } from "react-router-dom";

import { adminClient } from "../admin-client";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { toCustomUserFederation } from "../user-federation/routes/CustomUserFederation";
import { useFetch } from "../utils/useFetch";

type FederatedUserLinkProps = {
  user: UserRepresentation;
};

export const FederatedUserLink = ({ user }: FederatedUserLinkProps) => {
  const access = useAccess();
  const { realm } = useRealm();

  const [component, setComponent] = useState<ComponentRepresentation>();

  useFetch(
    () =>
      access.hasAccess("view-realm")
        ? adminClient.components.findOne({
            id: (user.federationLink || user.origin)!,
          })
        : adminClient.userStorageProvider.name({
            id: (user.federationLink || user.origin)!,
          }),
    setComponent,
    [],
  );

  if (!component) return null;

  return (
    <Button
      variant="link"
      isDisabled={!access.hasAccess("view-realm")}
      component={(props) => (
        <Link
          {...props}
          to={toCustomUserFederation({
            id: component.id!,
            providerId: component.providerId!,
            realm,
          })}
        />
      )}
    >
      {component.name}
    </Button>
  );
};
