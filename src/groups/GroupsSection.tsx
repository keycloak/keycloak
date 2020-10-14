import React, { useContext, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { HttpClientContext } from "../context/http-service/HttpClientContext";
import { GroupsList } from "./GroupsList";
import { GroupsCreateModal } from "./GroupsCreateModal";
import { GroupRepresentation } from "./models/groups";
import {
  ServerGroupsArrayRepresentation,
  ServerGroupMembersRepresentation,
} from "./models/server-info";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import {
  Button,
  Dropdown,
  DropdownItem,
  KebabToggle,
  PageSection,
  PageSectionVariants,
  Spinner,
  ToolbarItem,
} from "@patternfly/react-core";
import "./GroupsSection.css";

export const GroupsSection = () => {
  const { t } = useTranslation("groups");
  const httpClient = useContext(HttpClientContext)!;
  const [rawData, setRawData] = useState<{ [key: string]: any }[]>();
  const [filteredData, setFilteredData] = useState<object[]>();
  const [isKebabOpen, setIsKebabOpen] = useState(false);
  const [createGroupName, setCreateGroupName] = useState("");
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const columnID: keyof GroupRepresentation = "id";
  const membersLength: keyof GroupRepresentation = "membersLength";
  const columnGroupName: keyof GroupRepresentation = "name";

  const loader = async () => {
    const groups = await httpClient.doGet<ServerGroupsArrayRepresentation[]>(
      "/admin/realms/master/groups"
    );
    const groupsData = groups.data!;

    const getMembers = async (id: number) => {
      const response = await httpClient.doGet<
        ServerGroupMembersRepresentation[]
      >(`/admin/realms/master/groups/${id}/members`);
      const responseData = response.data!;
      return responseData.length;
    };

    const memberPromises = groupsData.map((group: { [key: string]: any }) =>
      getMembers(group[columnID])
    );
    const memberData = await Promise.all(memberPromises);
    const updatedObject = groupsData.map(
      (group: { [key: string]: any }, i: number) => {
        const object = Object.assign({}, group);
        object[membersLength] = memberData[i];
        return object;
      }
    );

    setRawData(updatedObject);
  };

  useEffect(() => {
    loader();
  }, []);

  // Filter groups
  const filterGroups = (newInput: string) => {
    const localRowData = rawData!.filter((obj: { [key: string]: string }) => {
      const groupName = obj[columnGroupName];
      return groupName.toLowerCase().includes(newInput.toLowerCase());
    });
    setFilteredData(localRowData);
  };

  // Kebab delete action
  const onKebabToggle = (isOpen: boolean) => {
    setIsKebabOpen(isOpen);
  };

  const onKebabSelect = () => {
    setIsKebabOpen(!isKebabOpen);
  };

  const handleModalToggle = () => {
    setIsCreateModalOpen(!isCreateModalOpen);
  };

  return (
    <>
      <ViewHeader titleKey="groups:groups" subKey="groups:groupsDescription" />
      <PageSection variant={PageSectionVariants.light}>
        {!rawData && (
          <div className="pf-u-text-align-center">
            <Spinner />
          </div>
        )}
        {rawData && rawData.length > 0 ? (
          <>
            <TableToolbar
              inputGroupName="groupsToolbarTextInput"
              inputGroupPlaceholder={t("searchGroups")}
              inputGroupOnChange={filterGroups}
              toolbarItem={
                <>
                  <ToolbarItem>
                    <Button
                      variant="primary"
                      onClick={() => handleModalToggle()}
                    >
                      {t("createGroup")}
                    </Button>
                  </ToolbarItem>
                  <ToolbarItem>
                    <Dropdown
                      onSelect={onKebabSelect}
                      toggle={<KebabToggle onToggle={onKebabToggle} />}
                      isOpen={isKebabOpen}
                      isPlain
                      dropdownItems={[
                        <DropdownItem key="action" component="button">
                          {t("delete")}
                        </DropdownItem>,
                      ]}
                    />
                  </ToolbarItem>
                </>
              }
            >
              {rawData && (
                <GroupsList
                  list={filteredData ? filteredData : rawData}
                  refresh={loader}
                />
              )}
              {filteredData && filteredData.length === 0 && (
                <ListEmptyState
                  hasIcon={true}
                  isSearchVariant={true}
                  message={t("noSearchResults")}
                  instructions={t("noSearchResultsInstructions")}
                />
              )}
            </TableToolbar>
            <GroupsCreateModal
              isCreateModalOpen={isCreateModalOpen}
              handleModalToggle={handleModalToggle}
              setIsCreateModalOpen={setIsCreateModalOpen}
              createGroupName={createGroupName}
              setCreateGroupName={setCreateGroupName}
              refresh={loader}
            />
          </>
        ) : (
          <ListEmptyState
            hasIcon={true}
            message={t("noGroupsInThisRealm")}
            instructions={t("noGroupsInThisRealmInstructions")}
            primaryActionText={t("createGroup")}
          />
        )}
      </PageSection>
    </>
  );
};
