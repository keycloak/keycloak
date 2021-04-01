import React, { useEffect, useState } from "react";
import KeycloakAdminClient from "keycloak-admin";
import { Label } from "@patternfly/react-core";

export type AliasRendererComponentProps = {
  name?: string;
  containerId?: string;
  filterType?: string;
  adminClient: KeycloakAdminClient;
  id: string;
};

export const AliasRendererComponent = ({
  name,
  containerId,
  filterType,
  adminClient,
  id,
}: AliasRendererComponentProps) => {
  const [containerName, setContainerName] = useState<string>("");

  useEffect(() => {
    if (filterType === "clients") {
      adminClient.clients
        .findOne({ id: containerId! })
        .then((client) => setContainerName(client.clientId! as string));
    }
  }, [containerId]);

  if (filterType === "roles" || !containerName) {
    return <>{name}</>;
  }

  if (filterType === "clients" || containerName) {
    return (
      <>
        {containerId && (
          <Label color="blue" key={`label-${id}`}>
            {containerName}
          </Label>
        )}{" "}
        {name}
      </>
    );
  }

  return null;
};
