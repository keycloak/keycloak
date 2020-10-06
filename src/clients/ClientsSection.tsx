import React, { useState, useContext, useEffect } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button, PageSection, Spinner } from "@patternfly/react-core";

import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { ClientList } from "./ClientList";
import { HttpClientContext } from "../context/http-service/HttpClientContext";
import { KeycloakContext } from "../context/auth/KeycloakContext";
import { ClientRepresentation } from "./models/client-model";
import { RealmContext } from "../context/realm-context/RealmContext";
import { ViewHeader } from "../components/view-header/ViewHeader";

export const ClientsSection = () => {
  const { t } = useTranslation("clients");
  const history = useHistory();

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const [search, setSearch] = useState("");
  const [clients, setClients] = useState<ClientRepresentation[]>();
  const httpClient = useContext(HttpClientContext)!;
  const keycloak = useContext(KeycloakContext);
  const { realm } = useContext(RealmContext);

  const loader = async () => {
    const params: { [name: string]: string | number } = { first, max };
    if (search) {
      params.clientId = search;
      params.search = "true";
    }
    const result = await httpClient.doGet<ClientRepresentation[]>(
      `/admin/realms/${realm}/clients`,
      { params: params }
    );
    setClients(result.data);
  };

  useEffect(() => {
    loader();
  }, []);

  return (
    <>
      <ViewHeader
        titleKey="clients:clientList"
        subKey="clients:clientsExplain"
      />
      <PageSection variant="light">
        {!clients && (
          <div style={{ textAlign: "center" }}>
            <Spinner />
          </div>
        )}
        {clients && (
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
            inputGroupOnChange={setSearch}
            inputGroupOnClick={() => loader()}
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
              refresh={loader}
              baseUrl={keycloak!.authServerUrl()!}
            />
          </TableToolbar>
        )}
      </PageSection>
    </>
  );
};
