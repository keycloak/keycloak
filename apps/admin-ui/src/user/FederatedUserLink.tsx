import { Button } from "@patternfly/react-core";
import { useState } from "react";
import { Link } from "react-router-dom-v5-compat";

import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useAccess } from "../context/access/Access";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { toUserFederationLdap } from "../user-federation/routes/UserFederationLdap";

type FederatedUserLinkProps = {
  user: UserRepresentation;
};

export const FederatedUserLink = ({ user }: FederatedUserLinkProps) => {
  const access = useAccess();
  const { realm } = useRealm();
  const { adminClient } = useAdminClient();

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
    []
  );

  if (!component) return null;

  return (
    <Button
      variant="link"
      isDisabled={!access.hasAccess("view-realm")}
      component={(props) => (
        <Link
          {...props}
          to={toUserFederationLdap({
            id: component.id!,
            realm,
          })}
        />
      )}
    >
      {component.name}
    </Button>
  );
};
