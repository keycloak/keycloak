import {
  Button,
  PageSection,
  PageSectionVariants,
} from "@patternfly/react-core";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { FederatedUserLink } from "../FederatedUserLink";

type FederatedCredentialsProps = {
  user: UserRepresentation;
  onSetPassword: () => void;
};

export const FederatedCredentials = ({
  user,
  onSetPassword,
}: FederatedCredentialsProps) => {
  const { t } = useTranslation("users");
  const { adminClient } = useAdminClient();

  const [credentialTypes, setCredentialTypes] = useState<string[]>();

  useFetch(
    () => adminClient.users.getUserStorageCredentialTypes({ id: user.id! }),
    setCredentialTypes,
    []
  );

  if (!credentialTypes) {
    return <KeycloakSpinner />;
  }

  return (
    <PageSection variant={PageSectionVariants.light}>
      <TableComposable variant={"compact"}>
        <Thead>
          <Tr>
            <Th>{t("type")}</Th>
            <Th>{t("providedBy")}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {credentialTypes.map((credential) => (
            <Tr key={credential}>
              <Td>
                <b>{credential}</b>
              </Td>
              <Td>
                <FederatedUserLink user={user} />
              </Td>
              {credential === "password" && (
                <Td modifier="fitContent">
                  <Button variant="secondary" onClick={onSetPassword}>
                    {t("setPassword")}
                  </Button>
                </Td>
              )}
            </Tr>
          ))}
        </Tbody>
      </TableComposable>
    </PageSection>
  );
};
