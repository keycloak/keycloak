import { DataList, Stack, StackItem, Title } from "@patternfly/react-core";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { getLinkedAccounts } from "../api/methods";
import { LinkedAccountRepresentation } from "../api/representations";
import { EmptyRow } from "../components/datalist/EmptyRow";
import { Page } from "../components/page/Page";
import { usePromise } from "../utils/usePromise";
import { AccountRow } from "./AccountRow";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";

export const LinkedAccounts = () => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const [accounts, setAccounts] = useState<LinkedAccountRepresentation[]>([]);

  const [key, setKey] = useState(1);
  const refresh = () => setKey(key + 1);

  usePromise((signal) => getLinkedAccounts({ signal, context }), setAccounts, [
    key,
  ]);

  const linkedAccounts = useMemo(
    () => accounts.filter((account) => account.connected),
    [accounts],
  );

  const unLinkedAccounts = useMemo(
    () => accounts.filter((account) => !account.connected),
    [accounts],
  );

  return (
    <Page
      title={t("linkedAccounts")}
      description={t("linkedAccountsIntroMessage")}
    >
      <Stack hasGutter>
        <StackItem>
          <Title headingLevel="h2" className="pf-v5-u-mb-lg" size="xl">
            {t("linkedLoginProviders")}
          </Title>
          <DataList id="linked-idps" aria-label={t("linkedLoginProviders")}>
            {linkedAccounts.length > 0 ? (
              linkedAccounts.map((account) => (
                <AccountRow
                  key={account.providerName}
                  account={account}
                  isLinked
                  refresh={refresh}
                />
              ))
            ) : (
              <EmptyRow message={t("linkedEmpty")} />
            )}
          </DataList>
        </StackItem>
        <StackItem>
          <Title
            headingLevel="h2"
            className="pf-v5-u-mt-xl pf-v5-u-mb-lg"
            size="xl"
          >
            {t("unlinkedLoginProviders")}
          </Title>
          <DataList id="unlinked-idps" aria-label={t("unlinkedLoginProviders")}>
            {unLinkedAccounts.length > 0 ? (
              unLinkedAccounts.map((account) => (
                <AccountRow
                  key={account.providerName}
                  account={account}
                  refresh={refresh}
                />
              ))
            ) : (
              <EmptyRow message={t("unlinkedEmpty")} />
            )}
          </DataList>
        </StackItem>
      </Stack>
    </Page>
  );
};

export default LinkedAccounts;
