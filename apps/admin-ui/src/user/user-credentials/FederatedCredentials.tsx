import { useState } from "react";
import { useTranslation } from "react-i18next";
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

import type ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { KeycloakSpinner } from "../../components/keycloak-spinner/KeycloakSpinner";
import { useAccess } from "../../context/access/Access";
import { toUserFederationLdap } from "../../user-federation/routes/UserFederationLdap";
import { useRealm } from "../../context/realm-context/RealmContext";
import { Link } from "react-router-dom";

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
  const { realm } = useRealm();

  const [credentialTypes, setCredentialTypes] = useState<string[]>();
  const [component, setComponent] = useState<ComponentRepresentation>();
  const access = useAccess();

  useFetch(
    () =>
      Promise.all([
        adminClient.users.getUserStorageCredentialTypes({ id: user.id! }),
        access.hasAccess("view-realm")
          ? adminClient.components.findOne({
              id: (user.federationLink || user.origin)!,
            })
          : adminClient.userStorageProvider.name({
              id: (user.federationLink || user.origin)!,
            }),
      ]),
    ([credentialTypes, component]) => {
      setCredentialTypes(credentialTypes);
      setComponent(component);
    },
    []
  );

  if (!credentialTypes || !component) {
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
