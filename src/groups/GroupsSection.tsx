import React, {
  createContext,
  ReactNode,
  useContext,
  useEffect,
  useState,
} from "react";
import { Link, useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Button,
  Dropdown,
  DropdownItem,
  KebabToggle,
  PageSection,
  PageSectionVariants,
  ToolbarItem,
  AlertVariant,
} from "@patternfly/react-core";
import { UsersIcon } from "@patternfly/react-icons";
import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";

import { GroupsCreateModal } from "./GroupsCreateModal";
import { ViewHeader } from "../components/view-header/ViewHeader";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";

import "./GroupsSection.css";
import { useRealm } from "../context/realm-context/RealmContext";

type GroupTableData = GroupRepresentation & {
  membersLength?: number;
};

type SubGroupsProps = {
  subGroups: GroupRepresentation[];
  setSubGroups: (group: GroupRepresentation[]) => void;
  clear: () => void;
  remove: (group: GroupRepresentation) => void;
};

const SubGroupContext = createContext<SubGroupsProps>({
  subGroups: [],
  setSubGroups: () => {},
  clear: () => {},
  remove: () => {},
});

export const SubGroups = ({ children }: { children: ReactNode }) => {
  const [subGroups, setSubGroups] = useState<GroupRepresentation[]>([]);

  const clear = () => setSubGroups([]);
  const remove = (group: GroupRepresentation) =>
    setSubGroups(
      subGroups.slice(
        0,
        subGroups.findIndex((g) => g.id === group.id)
      )
    );
  return (
    <SubGroupContext.Provider
      value={{ subGroups, setSubGroups, clear, remove }}
    >
      {children}
    </SubGroupContext.Provider>
  );
};

export const useSubGroups = () => useContext(SubGroupContext);

const getId = (pathname: string) => {
  const pathParts = pathname.substr(1).split("/");
  return pathParts.length > 1 ? pathParts.splice(2) : undefined;
};

const getLastId = (pathname: string) => {
  const pathParts = getId(pathname);
  return pathParts ? pathParts[pathParts.length - 1] : undefined;
};

export const GroupsSection = () => {
  const { t } = useTranslation("groups");
  const adminClient = useAdminClient();
  const [isKebabOpen, setIsKebabOpen] = useState(false);
  const [createGroupName, setCreateGroupName] = useState("");
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedRows, setSelectedRows] = useState<GroupRepresentation[]>([]);
  const { subGroups, setSubGroups } = useSubGroups();
  const { addAlert } = useAlerts();
  const { realm } = useRealm();
  const history = useHistory();

  const location = useLocation();
  const id = getLastId(location.pathname);

  const [key, setKey] = useState("");
  const refresh = () => setKey(`${new Date().getTime()}`);

  const getMembers = async (id: string) => {
    const response = await adminClient.groups.listMembers({ id });
    return response ? response.length : 0;
  };

  const loader = async () => {
    let groupsData;
    if (!id) {
      groupsData = await adminClient.groups.find();
    } else {
      const ids = getId(location.pathname);
      const isNavigationStateInValid = ids && ids.length !== subGroups.length;
      if (isNavigationStateInValid) {
        const groups = [];
        for (const i of ids!) {
          const group = await adminClient.groups.findOne({ id: i });
          if (group) groups.push(group);
        }
        setSubGroups(groups);
        groupsData = groups.pop()?.subGroups!;
      } else {
        const group = await adminClient.groups.findOne({ id });
        if (group) {
          setSubGroups([...subGroups, group]);
          groupsData = group.subGroups!;
        }
      }
    }

    if (groupsData) {
      const memberPromises = groupsData.map((group) => getMembers(group.id!));
      const memberData = await Promise.all(memberPromises);
      return groupsData.map((group: GroupTableData, i) => {
        group.membersLength = memberData[i];
        return group;
      });
    } else {
      history.push(`/${realm}/groups`);
    }

    return [];
  };

  useEffect(() => {
    refresh();
  }, [id]);

  const handleModalToggle = () => {
    setIsCreateModalOpen(!isCreateModalOpen);
  };

  const deleteGroup = async (group: GroupRepresentation) => {
    try {
      await adminClient.groups.del({
        id: group.id!,
      });
      addAlert(t("groupDelete"), AlertVariant.success);
    } catch (error) {
      addAlert(t("groupDeleteError", { error }), AlertVariant.danger);
    }
    return true;
  };

  const multiDelete = async () => {
    if (selectedRows!.length !== 0) {
      const chainedPromises = selectedRows!.map((group) => deleteGroup(group));

      await Promise.all(chainedPromises);
      addAlert(t("groupsDeleted"), AlertVariant.success);
      setSelectedRows([]);
      refresh();
    }
  };

  const GroupNameCell = (group: GroupTableData) => (
    <>
      <Link key={group.id} to={`${location.pathname}/${group.id}`}>
        {group.name}
      </Link>
    </>
  );

  const GroupMemberCell = (group: GroupTableData) => (
    <div className="keycloak-admin--groups__member-count">
      <UsersIcon key={`user-icon-${group.id}`} />
      {group.membersLength}
    </div>
  );

  return (
    <>
      <ViewHeader titleKey="groups:groups" subKey="groups:groupsDescription" />
      <PageSection variant={PageSectionVariants.light}>
        <KeycloakDataTable
          key={key}
          onSelect={(rows) => setSelectedRows([...rows])}
          canSelectAll={false}
          loader={loader}
          ariaLabelKey="groups:groups"
          searchPlaceholderKey="groups:searchForGroups"
          toolbarItem={
            <>
              <ToolbarItem>
                <Button variant="primary" onClick={handleModalToggle}>
                  {t("createGroup")}
                </Button>
              </ToolbarItem>
              <ToolbarItem>
                <Dropdown
                  toggle={
                    <KebabToggle
                      onToggle={() => setIsKebabOpen(!isKebabOpen)}
                    />
                  }
                  isOpen={isKebabOpen}
                  isPlain
                  dropdownItems={[
                    <DropdownItem
                      key="action"
                      component="button"
                      onClick={() => {
                        multiDelete();
                        setIsKebabOpen(false);
                      }}
                    >
                      {t("common:delete")}
                    </DropdownItem>,
                  ]}
                />
              </ToolbarItem>
            </>
          }
          actions={[
            {
              title: t("moveTo"),
              onRowClick: () => console.log("TO DO: Add move to functionality"),
            },
            {
              title: t("common:delete"),
              onRowClick: async (group: GroupRepresentation) => {
                return deleteGroup(group);
              },
            },
          ]}
          columns={[
            {
              name: "name",
              displayKey: "groups:groupName",
              cellRenderer: GroupNameCell,
            },
            {
              name: "members",
              displayKey: "groups:members",
              cellRenderer: GroupMemberCell,
            },
          ]}
          emptyState={
            <ListEmptyState
              hasIcon={true}
              message={t("noGroupsInThisRealm")}
              instructions={t("noGroupsInThisRealmInstructions")}
              primaryActionText={t("createGroup")}
              onPrimaryAction={() => handleModalToggle()}
            />
          }
        />

        <GroupsCreateModal
          isCreateModalOpen={isCreateModalOpen}
          handleModalToggle={handleModalToggle}
          setIsCreateModalOpen={setIsCreateModalOpen}
          createGroupName={createGroupName}
          setCreateGroupName={setCreateGroupName}
          refresh={refresh}
        />
      </PageSection>
    </>
  );
};
