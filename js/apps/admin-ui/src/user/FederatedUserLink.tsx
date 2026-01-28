import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useFetch } from "@keycloak/keycloak-ui-shared";
import { Button } from "@patternfly/react-core";
import { useState } from "react";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import { useAccess } from "../context/access/Access";
import { useRealm } from "../context/realm-context/RealmContext";
import { toCustomUserFederation } from "../user-federation/routes/CustomUserFederation";

type FederatedUserLinkProps = {
  user: UserRepresentation;
};

export const FederatedUserLink = ({ user }: FederatedUserLinkProps) => {
  const { adminClient } = useAdminClient();

  const access = useAccess();
  const { realm } = useRealm();

  const [component, setComponent] = useState<ComponentRepresentation>();

  useFetch(
    () =>
      access.hasAccess("view-realm")
        ? adminClient.components.findOne({
            id: user.federationLink!,
          })
        : adminClient.userStorageProvider.name({
            id: user.federationLink!,
          }),
    setComponent,
    [],
  );

  if (!component) return null;

  if (!access.hasAccess("view-realm")) return <span>{component.name}</span>;

  return (
    <Button
      variant="link"
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
