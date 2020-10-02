import React, { useContext, useState } from "react";
import { Button, PageSection } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

import { RealmContext } from "../components/realm-context/RealmContext";
import { HttpClientContext } from "../http-service/HttpClientContext";
import { ClientRepresentation } from "../realm/models/Realm";
import { DataLoader } from "../components/data-loader/DataLoader";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { ClientScopeList } from "./ClientScopesList";
import { ViewHeader } from "../components/view-header/ViewHeader";

export const ClientScopesSection = () => {
  const { t } = useTranslation("client-scopes");
  const history = useHistory();

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);

  const loader = async () => {
    return await httpClient
      .doGet(`/admin/realms/${realm}/client-scopes`, { params: { first, max } })
      .then((r) => r.data as ClientRepresentation[]);
  };
  return (
    <>
      <ViewHeader
        titleKey="clientScopes"
        subKey="client-scopes:clientScopeExplain"
      />
      <PageSection variant="light">
        <DataLoader loader={loader}>
          {(scopes) => (
            <TableToolbar
              count={scopes!.length}
              first={first}
              max={max}
              onNextClick={setFirst}
              onPreviousClick={setFirst}
              onPerPageSelect={(first, max) => {
                setFirst(first);
                setMax(max);
              }}
              toolbarItem={
                <Button onClick={() => history.push("/add-client-scopes")}>
                  {t("createClientScope")}
                </Button>
              }
            >
              <ClientScopeList clientScopes={scopes} />
            </TableToolbar>
          )}
        </DataLoader>
      </PageSection>
    </>
  );
};
