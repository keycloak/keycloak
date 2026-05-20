import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import {
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Stack,
  StackItem,
  Title,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { getVerifiableCredentials } from "../api/methods";
import { UserVerifiableCredentialRepresentation } from "../api/representations";
import { EmptyRow } from "../components/datalist/EmptyRow";
import { Page } from "../components/page/Page";
import { usePromise } from "../utils/usePromise";
import { CredentialRow } from "./CredentialRow";

export const VerifiableCredentials = () => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const [credentials, setCredentials] = useState<
    UserVerifiableCredentialRepresentation[]
  >([]);
  const [key, setKey] = useState(1);
  const refresh = () => setKey(key + 1);

  usePromise(
    (signal) => getVerifiableCredentials({ signal, context }),
    (data) => setCredentials(data),
    [key],
  );

  return (
    <Page
      title={t("verifiableCredentials")}
      description={t("verifiableCredentialsDescription")}
    >
      <Stack hasGutter>
        <StackItem>
          <Title headingLevel="h2" className="pf-v5-u-mb-lg" size="xl">
            {t("myVerifiableCredentials")}
          </Title>
          <DataList
            id="verifiable-credentials"
            aria-label={t("verifiableCredentials")}
            isCompact
          >
            <DataListItem
              id="verifiable-credentials-header"
              aria-labelledby="Column headers"
            >
              <DataListItemRow>
                <DataListItemCells
                  dataListCells={[
                    <DataListCell key="credential-name-header" width={2}>
                      <strong>{t("credentialScopeName")}</strong>
                    </DataListCell>,
                    <DataListCell key="credential-created-header" width={2}>
                      <strong>{t("credentialCreatedDate")}</strong>
                    </DataListCell>,
                  ]}
                />
              </DataListItemRow>
            </DataListItem>
            {credentials.length > 0 ? (
              credentials.map((credential) => (
                <CredentialRow
                  key={credential.credentialScopeName}
                  credential={credential}
                  refresh={refresh}
                />
              ))
            ) : (
              <EmptyRow message={t("noVerifiableCredentials")} />
            )}
          </DataList>
        </StackItem>
      </Stack>
    </Page>
  );
};

export default VerifiableCredentials;
