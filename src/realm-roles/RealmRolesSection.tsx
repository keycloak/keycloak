import React, { useContext, useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Bullseye, Button, PageSection, Spinner } from "@patternfly/react-core";

import { HttpClientContext } from "../context/http-service/HttpClientContext";
import { RoleRepresentation } from "../model/role-model";
import { RolesList } from "./RoleList";
import { RealmContext } from "../context/realm-context/RealmContext";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { PaginatingTableToolbar } from "../components/table-toolbar/PaginatingTableToolbar";

export const RealmRolesSection = () => {
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);
  const { t } = useTranslation("roles");
  const history = useHistory();
  const httpClient = useContext(HttpClientContext)!;
  const [roles, setRoles] = useState<RoleRepresentation[]>();
  const { realm } = useContext(RealmContext);

  const loader = async () => {
    const params: { [name: string]: string | number } = { first, max };

    const result = await httpClient.doGet<RoleRepresentation[]>(
      `/admin/realms/${realm}/roles`,
      { params: params }
    );
    setRoles(result.data);
  };

  useEffect(() => {
    loader();
  }, [first, max]);

  return (
    <>
      <ViewHeader titleKey="roles:title" subKey="roles:roleExplain" />
      <PageSection variant="light">
        {!roles && (
          <Bullseye>
            <Spinner />
          </Bullseye>
        )}
        {roles && (
          <PaginatingTableToolbar
            count={roles!.length}
            first={first}
            max={max}
            onNextClick={setFirst}
            onPreviousClick={setFirst}
            onPerPageSelect={(first, max) => {
              setFirst(first);
              setMax(max);
            }}
            toolbarItem={
              <>
                <Button onClick={() => history.push("/add-role")}>
                  {t("createRole")}
                </Button>
              </>
            }
          >
            <RolesList roles={roles} refresh={loader} />
          </PaginatingTableToolbar>
        )}
      </PageSection>
    </>
  );
};
