import React from "react";
import moment from "moment";

import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";

export const InitialAccessTokenList = () => {
  const adminClient = useAdminClient();
  const { realm } = useRealm();
  const loader = async () =>
    await adminClient.realms.getClientsInitialAccess({ realm });

  return (
    <KeycloakDataTable
      ariaLabelKey="clients:initialAccessToken"
      searchPlaceholderKey="clients:searchInitialAccessToken"
      loader={loader}
      columns={[
        {
          name: "id",
        },
        {
          name: "timestamp",
          cellRenderer: (row) => moment(row.timestamp * 1000).fromNow(),
        },
        {
          name: "expiration",
          cellRenderer: (row) =>
            moment(moment.now() - row.expiration * 1000).fromNow(),
        },
        {
          name: "count",
        },
        {
          name: "remainingCount",
        },
      ]}
    />
  );
};
