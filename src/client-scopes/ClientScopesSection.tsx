import React, { useContext, useEffect, useState } from "react";
import { Button, PageSection, Spinner } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

import { RealmContext } from "../context/realm-context/RealmContext";
import { HttpClientContext } from "../context/http-service/HttpClientContext";
import { ClientRepresentation } from "../realm/models/Realm";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { ClientScopeList } from "./ClientScopesList";
import { ViewHeader } from "../components/view-header/ViewHeader";

export const ClientScopesSection = () => {
  const { t } = useTranslation("client-scopes");
  const history = useHistory();
  const [rawData, setRawData] = useState<ClientRepresentation[]>();
  const [filteredData, setFilteredData] = useState<ClientRepresentation[]>();

  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);

  useEffect(() => {
    (async () => {
      if (filteredData) {
        return filteredData;
      }
      const result = await httpClient.doGet<ClientRepresentation[]>(
        `/admin/realms/${realm}/client-scopes`
      );
      setRawData(result.data!);
    })();
  }, []);

  const filterData = (search: string) => {
    setFilteredData(
      rawData!.filter((group) =>
        group.name.toLowerCase().includes(search.toLowerCase())
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
              <Button
                onClick={() =>
                  history.push("/client-scopes/add-client-scopes/")
                }
              >
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
