import React, { useContext } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Button, PageSection } from "@patternfly/react-core";

import { DataLoader } from "../components/data-loader/DataLoader";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { HttpClientContext } from "../context/http-service/HttpClientContext";
import { RoleRepresentation } from "../model/role-model";
import { RolesList } from "./RoleList";
import { RealmContext } from "../context/realm-context/RealmContext";
import { ViewHeader } from "../components/view-header/ViewHeader";

export const RealmRolesSection = () => {
  const { t } = useTranslation("roles");
  const history = useHistory();
  const httpClient = useContext(HttpClientContext)!;
  const { realm } = useContext(RealmContext);

  const loader = async () => {
    const result = await httpClient.doGet<RoleRepresentation[]>(
      `/admin/realms/${realm}/roles`
    );
    return result.data;
  };

  return (
    <>
      <ViewHeader titleKey="roles:title" subKey="roles:roleExplain" />
      <PageSection padding={{ default: "noPadding" }}>
        <TableToolbar
          toolbarItem={
            <>
              <Button onClick={() => history.push("/add-role")}>
                {t("createRole")}
              </Button>
            </>
          }
        >
          <DataLoader loader={loader}>
            {(roles) => <RolesList roles={roles.data} />}
          </DataLoader>
        </TableToolbar>
      </PageSection>
    </>
  );
};
