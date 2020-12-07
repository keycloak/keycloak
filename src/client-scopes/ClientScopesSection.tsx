import React from "react";
import { useTranslation } from "react-i18next";
import { Link, useHistory } from "react-router-dom";
import { Button, PageSection } from "@patternfly/react-core";
import ClientScopeRepresentation from "keycloak-admin/lib/defs/clientScopeRepresentation";

import { useAdminClient } from "../context/auth/AdminClient";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { DataList } from "../components/table-toolbar/DataList";

export const ClientScopesSection = () => {
  const { t } = useTranslation("client-scopes");
  const history = useHistory();

  const adminClient = useAdminClient();

  const loader = async () => await adminClient.clientScopes.find();

  const ClientScopeDetailLink = (clientScope: ClientScopeRepresentation) => (
    <>
      <Link key={clientScope.id} to={`/client-scopes/${clientScope.id}`}>
        {clientScope.name}
      </Link>
    </>
  );
  return (
    <>
      <ViewHeader
        titleKey="clientScopes"
        subKey="client-scopes:clientScopeExplain"
      />
      <PageSection variant="light">
        <DataList
          loader={loader}
          ariaLabelKey="client-scopes:clientScopeList"
          searchPlaceholderKey="client-scopes:searchFor"
          toolbarItem={
            <Button onClick={() => history.push("/client-scopes/new")}>
              {t("createClientScope")}
            </Button>
          }
          actions={[
            {
              title: t("common:export"),
              onRowClick: () => {},
            },
            {
              title: t("common:delete"),
              onRowClick: () => {},
            },
          ]}
          columns={[
            {
              name: "name",
              cellRenderer: ClientScopeDetailLink,
            },
            { name: "description" },
            {
              name: "protocol",
            },
          ]}
        />
      </PageSection>
    </>
  );
};
