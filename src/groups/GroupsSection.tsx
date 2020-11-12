import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { GroupsList } from "./GroupsList";
import { GroupsCreateModal } from "./GroupsCreateModal";
import { TableToolbar } from "../components/table-toolbar/TableToolbar";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import {
  Button,
  Dropdown,
  DropdownItem,
  KebabToggle,
  PageSection,
  PageSectionVariants,
  Spinner,
  ToolbarItem,
  AlertVariant,
} from "@patternfly/react-core";
import "./GroupsSection.css";
import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";

export const GroupsSection = () => {
  const { t } = useTranslation("groups");
  const adminClient = useAdminClient();
  const [rawData, setRawData] = useState<{ [key: string]: any }[]>();
  const [filteredData, setFilteredData] = useState<{ [key: string]: any }[]>();
  const [isKebabOpen, setIsKebabOpen] = useState(false);
  const [createGroupName, setCreateGroupName] = useState("");
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [tableRowSelectedArray, setTableRowSelectedArray] = useState<
    Array<number>
  >([]);
  const columnID: keyof GroupRepresentation = "id";
  const columnGroupName: keyof GroupRepresentation = "name";
  const { addAlert } = useAlerts();
  const membersLength = "membersLength";

  const loader = async () => {
    const groupsData = await adminClient.groups.find();

    const getMembers = async (id: string) => {
      const response = await adminClient.groups.listMembers({ id });
      return response.length;
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
    setFilteredData(updatedObject);
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

  const multiDelete = async () => {
    if (tableRowSelectedArray.length !== 0) {
      const deleteGroup = async (rowId: number) => {
        try {
          await adminClient.groups.del({
            id: filteredData ? filteredData![rowId].id : rawData![rowId].id,
          });
          loader();
        } catch (error) {
          addAlert(`${t("groupDeleteError")} ${error}`, AlertVariant.danger);
        }
      };

      const chainedPromises = tableRowSelectedArray.map((rowId: number) => {
        deleteGroup(rowId);
      });

      await Promise.all(chainedPromises)
        .then(() => addAlert(t("groupsDeleted"), AlertVariant.success))
        .then(() => setTableRowSelectedArray([]));
    }
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
                        <DropdownItem
                          key="action"
                          component="button"
                          onClick={() => multiDelete()}
                        >
                          {t("common:Delete")}
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
                  tableRowSelectedArray={tableRowSelectedArray}
                  setTableRowSelectedArray={setTableRowSelectedArray}
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
          </>
        ) : (
          <ListEmptyState
            hasIcon={true}
            message={t("noGroupsInThisRealm")}
            instructions={t("noGroupsInThisRealmInstructions")}
            primaryActionText={t("createGroup")}
            onPrimaryAction={() => handleModalToggle()}
          />
        )}
        <GroupsCreateModal
          isCreateModalOpen={isCreateModalOpen}
          handleModalToggle={handleModalToggle}
          setIsCreateModalOpen={setIsCreateModalOpen}
          createGroupName={createGroupName}
          setCreateGroupName={setCreateGroupName}
          refresh={loader}
        />
      </PageSection>
    </>
  );
};
