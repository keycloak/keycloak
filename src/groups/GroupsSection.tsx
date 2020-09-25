import React, { useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { Button, PageSection } from "@patternfly/react-core";

import { HttpClientContext } from "../http-service/HttpClientContext";
import { GroupsList } from "./GroupsList";
import { GroupsCreateModal } from "./GroupsCreateModal";
import { DataLoader } from "../components/data-loader/DataLoader";
import { GroupRepresentation } from "./models/groups";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";

export const GroupsSection = () => {
  const { t } = useTranslation("groups");
  const history = useHistory();
  const httpClient = useContext(HttpClientContext)!;
  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const handleModalToggle = () => {
    setIsCreateModalOpen(!isCreateModalOpen)
  };

  const loader = async () => {
    return await httpClient
      .doGet("/admin/realms/master/groups", { params: { first, max } })
      .then((r) => r.data as GroupRepresentation[]);
  };

  return (
    <>
      <PageSection variant="light">
        <DataLoader loader={loader}>
          {(groups) => (
            <TableToolbar
              count={groups!.length}
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
                  <Button onClick={() => handleModalToggle()}>
                    {t("Create group")}
                  </Button>
                </>
              }
            >
              <GroupsList list={groups} />
            </TableToolbar>
            
          )}
        </DataLoader>
        <GroupsCreateModal isCreateModalOpen={isCreateModalOpen} handleModalToggle={handleModalToggle}/>
      </PageSection>
    </>
  );
};
