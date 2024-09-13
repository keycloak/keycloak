import { DataList, Stack, StackItem, Title } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { getLinkedAccounts } from "../api/methods";
import { LinkedAccountRepresentation } from "../api/representations";
import { EmptyRow } from "../components/datalist/EmptyRow";
import { Page } from "../components/page/Page";
import { usePromise } from "../utils/usePromise";
import { AccountRow } from "./AccountRow";
import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import { LinkedAccountsToolbar } from "../resources/LinkedAccountsToolbar";

export const LinkedAccounts = () => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const [linkedAccounts, setLinkedAccounts] = useState<
    LinkedAccountRepresentation[]
  >([]);
  const [unlinkedAccounts, setUninkedAccounts] = useState<
    LinkedAccountRepresentation[]
  >([]);

  const [paramsUnlinked, setParamsUnlinked] = useState<Record<string, any>>({
    first: "0",
    max: "6",
    linked: false,
  });
  const [paramsLinked, setParamsLinked] = useState<Record<string, any>>({
    first: "0",
    max: "6",
    linked: true,
  });
  const [key, setKey] = useState(1);
  const refresh = () => setKey(key + 1);

  usePromise(
    (signal) => getLinkedAccounts({ signal, context }, paramsUnlinked),
    setUninkedAccounts,
    [paramsUnlinked, key],
  );

  usePromise(
    (signal) => getLinkedAccounts({ signal, context }, paramsLinked),
    setLinkedAccounts,
    [paramsLinked, key],
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
          <LinkedAccountsToolbar
            onFilter={(search) =>
              setParamsLinked({ ...paramsLinked, first: 0, search })
            }
            count={linkedAccounts.length}
            first={parseInt(paramsLinked["first"])}
            max={parseInt(paramsLinked["max"])}
            onNextClick={() => {
              setParamsLinked({
                ...paramsLinked,
                first:
                  parseInt(paramsLinked.first) + parseInt(paramsLinked.max) - 1,
              });
            }}
            onPreviousClick={() =>
              setParamsLinked({
                ...paramsLinked,
                first:
                  parseInt(paramsLinked.first) - parseInt(paramsLinked.max) + 1,
              })
            }
            onPerPageSelect={(first, max) =>
              setParamsLinked({
                ...paramsLinked,
                first: `${first}`,
                max: `${max}`,
              })
            }
            hasNext={linkedAccounts.length > paramsLinked.max - 1}
          />
          <DataList id="linked-idps" aria-label={t("linkedLoginProviders")}>
            {linkedAccounts.length > 0 ? (
              linkedAccounts.map(
                (account, index) =>
                  index !== paramsLinked.max - 1 && (
                    <AccountRow
                      key={account.providerName}
                      account={account}
                      isLinked
                      refresh={refresh}
                    />
                  ),
              )
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
          <LinkedAccountsToolbar
            onFilter={(search) =>
              setParamsUnlinked({ ...paramsUnlinked, first: 0, search })
            }
            count={unlinkedAccounts.length}
            first={parseInt(paramsUnlinked["first"])}
            max={parseInt(paramsUnlinked["max"])}
            onNextClick={() => {
              setParamsUnlinked({
                ...paramsUnlinked,
                first:
                  parseInt(paramsUnlinked.first) +
                  parseInt(paramsUnlinked.max) -
                  1,
              });
            }}
            onPreviousClick={() =>
              setParamsUnlinked({
                ...paramsUnlinked,
                first:
                  parseInt(paramsUnlinked.first) -
                  parseInt(paramsUnlinked.max) +
                  1,
              })
            }
            onPerPageSelect={(first, max) =>
              setParamsUnlinked({
                ...paramsUnlinked,
                first: `${first}`,
                max: `${max}`,
              })
            }
            hasNext={unlinkedAccounts.length > paramsUnlinked.max - 1}
          />
          <DataList id="unlinked-idps" aria-label={t("unlinkedLoginProviders")}>
            {unlinkedAccounts.length > 0 ? (
              unlinkedAccounts.map(
                (account, index) =>
                  index !== paramsUnlinked.max - 1 && (
                    <AccountRow
                      key={account.providerName}
                      account={account}
                      refresh={refresh}
                    />
                  ),
              )
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
