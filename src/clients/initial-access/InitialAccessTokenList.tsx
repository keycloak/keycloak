import React from "react";
import { useHistory, useRouteMatch } from "react-router-dom";
import moment from "moment";
import { useTranslation } from "react-i18next";
import { Button } from "@patternfly/react-core";

import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";

export const InitialAccessTokenList = () => {
  const { t } = useTranslation("clients");
  const adminClient = useAdminClient();
  const { realm } = useRealm();

  const history = useHistory();
  const { url } = useRouteMatch();

  const loader = async () =>
    await adminClient.realms.getClientsInitialAccess({ realm });

  return (
    <KeycloakDataTable
      ariaLabelKey="clients:initialAccessToken"
      searchPlaceholderKey="clients:searchInitialAccessToken"
      loader={loader}
      toolbarItem={
        <>
          <Button onClick={() => history.push(`${url}/create`)}>
            {t("common:create")}
          </Button>
        </>
      }
      columns={[
        {
          name: "id",
          displayKey: "clients:id",
        },
        {
          name: "timestamp",
          displayKey: "clients:timestamp",
          cellRenderer: (row) => moment(row.timestamp * 1000).format("LLL"),
        },
        {
          name: "expiration",
          displayKey: "clients:expires",
          cellRenderer: (row) =>
            moment(row.timestamp * 1000 + row.expiration * 1000).fromNow(),
        },
        {
          name: "count",
          displayKey: "clients:count",
        },
        {
          name: "remainingCount",
          displayKey: "clients:remainingCount",
        },
      ]}
    />
  );
};
