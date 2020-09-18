/* eslint-disable @typescript-eslint/no-unused-vars */
import React, { useState, useContext, useEffect } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Button,
  Divider,
  Page,
  PageSection,
  PageSectionVariants,
  Text,
  TextContent,
} from "@patternfly/react-core";

import { DataLoader } from "../components/data-loader/DataLoader";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { HttpClientContext } from "../http-service/HttpClientContext";
import { RoleRepresentation } from "../model/role-model";
import { RolesList } from "./RoleList";
import { RealmContext } from "../components/realm-context/RealmContext";

export const RealmRolesSection = () => {
  const { t } = useTranslation("roles");
  const history = useHistory();
  const [max, setMax] = useState(10);
  const [, setRoles] = useState([] as RoleRepresentation[]);
  const [first, setFirst] = useState(0);
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);

  const loader = async () => {
    return await httpClient
      .doGet(`/admin/realms/${realm}/roles`)
      .then((r) => r.data as RoleRepresentation[]);
  };

  useEffect(() => {
    loader().then((result) => setRoles(result || []));
  }, []);

  return (
    <DataLoader loader={loader}>
      {(roles) => (
        <>
          <PageSection variant="light">
            <TextContent>
              <Text component="h1">Realm roles</Text>
              <Text component="p">{t("roleExplain")}</Text>
            </TextContent>
          </PageSection>
          <Divider component="li" key={1} />
          <PageSection padding={{ default: "noPadding" }}>
            <TableToolbar
              count={roles!.length}
              first={first}
              max={max}
              onNextClick={setFirst}
              onPreviousClick={setFirst}
              onPerPageSelect={(f, m) => {
                setFirst(f);
                setMax(m);
              }}
              toolbarItem={
                <>
                  <Button onClick={() => history.push("/add-role")}>
                    {t("createRole")}
                  </Button>
                </>
              }
            >
              <RolesList roles={roles} />
            </TableToolbar>
          </PageSection>
        </>
      )}
    </DataLoader>
  );
};
