import React, { useState, useContext } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button, PageSection } from "@patternfly/react-core";

import { DataLoader } from "../components/data-loader/DataLoader";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { ClientList } from "./ClientList";
import { HttpClientContext } from "../http-service/HttpClientContext";
import { KeycloakContext } from "../auth/KeycloakContext";
import { ClientRepresentation } from "./models/client-model";
import { RealmContext } from "../components/realm-context/RealmContext";
import { ViewHeader } from "../components/view-header/ViewHeader";

export const ClientsSection = () => {
  const { t } = useTranslation("clients");
  const history = useHistory();

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const httpClient = useContext(HttpClientContext)!;
  const keycloak = useContext(KeycloakContext);
  const { realm } = useContext(RealmContext);

  const loader = async () => {
    return await httpClient
      .doGet(`/admin/realms/${realm}/clients`, { params: { first, max } })
      .then((r) => r.data as ClientRepresentation[]);
  };

  return (
    <>
      <ViewHeader
        titleKey="clients:clientList"
        subKey="clients:clientsExplain"
      />
      <PageSection variant="light">
        <DataLoader loader={loader}>
          {(clients) => (
            <TableToolbar
              count={clients!.length}
              first={first}
              max={max}
              onNextClick={setFirst}
              onPreviousClick={setFirst}
              onPerPageSelect={(first, max) => {
                setFirst(first);
                setMax(max);
              }}
              inputGroupName="clientsToolbarTextInput"
              inputGroupPlaceholder={t("Search for client")}
              toolbarItem={
                <>
                  <Button onClick={() => history.push("/add-client")}>
                    {t("createClient")}
                  </Button>
                  <Button
                    onClick={() => history.push("/import-client")}
                    variant="link"
                  >
                    {t("importClient")}
                  </Button>
                </>
              }
            >
              <ClientList
                clients={clients}
                baseUrl={keycloak!.authServerUrl()!}
              />
            </TableToolbar>
          )}
        </DataLoader>
      </PageSection>
    </>
  );
};
