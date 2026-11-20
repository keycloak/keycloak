import { useFetch } from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useWatch } from "react-hook-form";
import { useAdminClient } from "../../admin-client";
import { toRequestedCredentialsForm } from "../../identity-providers/add/requestedCredentials";
import type { IdentityProviderEditMapperParams } from "../../identity-providers/routes/EditMapper";
import { useParams } from "../../utils/useParams";
import type { ComponentProps } from "./components";
import { ListComponent } from "./ListComponent";

export const OID4VPCredentialListComponent = (props: ComponentProps) => {
  const { adminClient } = useAdminClient();
  const { alias } = useParams<IdentityProviderEditMapperParams>();
  const [credentialIds, setCredentialIds] = useState<string[]>();
  const selected: string | undefined = useWatch({
    name: props.convertToName(props.name!),
  });

  useFetch(
    () => adminClient.identityProviders.findOne({ alias }),
    (idp) =>
      setCredentialIds(
        toRequestedCredentialsForm(idp?.config?.requestedCredentials)
          .credentials.map(({ id }) => id)
          .filter((id) => id),
      ),
    [alias],
  );

  if (!credentialIds) return null;

  // A credential that is no longer requested stays visible, so the stale setting can be seen and changed
  const options =
    selected && !credentialIds.includes(selected)
      ? [selected, ...credentialIds]
      : credentialIds;

  return <ListComponent {...props} options={options} />;
};
