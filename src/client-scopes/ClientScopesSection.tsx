import React, { useEffect, useState } from "react";
import { Button, PageSection, Spinner } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

import ClientRepresentation from "keycloak-admin/lib/defs/clientRepresentation";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { ClientScopeList } from "./ClientScopesList";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { useAdminClient } from "../context/auth/AdminClient";

export const ClientScopesSection = () => {
  const { t } = useTranslation("client-scopes");
  const history = useHistory();
  const [rawData, setRawData] = useState<ClientRepresentation[]>();
  const [filteredData, setFilteredData] = useState<ClientRepresentation[]>();

  const adminClient = useAdminClient();

  useEffect(() => {
    (async () => {
      if (filteredData) {
        return filteredData;
      }
      const result = await adminClient.clientScopes.find();
      setRawData(result);
    })();
  }, []);

  const filterData = (search: string) => {
    setFilteredData(
      rawData!.filter((group) =>
        group.name!.toLowerCase().includes(search.toLowerCase())
      )
    );
  };
  return (
    <>
      <ViewHeader
        titleKey="clientScopes"
        subKey="client-scopes:clientScopeExplain"
      />
      <PageSection variant="light">
        {!rawData && (
          <div className="pf-u-text-align-center">
            <Spinner />
          </div>
        )}
        {rawData && (
          <TableToolbar
            inputGroupName="clientsScopeToolbarTextInput"
            inputGroupPlaceholder={t("searchFor")}
            inputGroupOnChange={filterData}
            toolbarItem={
              <Button onClick={() => history.push("/client-scopes/new")}>
                {t("createClientScope")}
              </Button>
            }
          >
            <ClientScopeList clientScopes={filteredData || rawData} />
          </TableToolbar>
        )}
      </PageSection>
    </>
  );
};
