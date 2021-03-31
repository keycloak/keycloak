import React, { useEffect, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useErrorHandler } from "react-error-boundary";
import {
  DropdownItem,
  PageSection,
  PageSectionVariants,
  AlertVariant,
  Tab,
  TabTitleText,
  Tabs,
} from "@patternfly/react-core";
import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";

import { ViewHeader } from "../components/view-header/ViewHeader";
import { asyncStateFetch, useAdminClient } from "../context/auth/AdminClient";
import { useAlerts } from "../components/alert/Alerts";
import { useRealm } from "../context/realm-context/RealmContext";

import { useSubGroups } from "./SubGroupsContext";
import { GroupTable } from "./GroupTable";
import { getId, getLastId } from "./groupIdUtils";
import { Members } from "./Members";
import { GroupAttributes } from "./GroupAttributes";
import { GroupsModal } from "./GroupsModal";

import "./GroupsSection.css";

export const GroupsSection = () => {
  const { t } = useTranslation("groups");
  const [activeTab, setActiveTab] = useState(0);

  const adminClient = useAdminClient();
  const { subGroups, setSubGroups, currentGroup } = useSubGroups();
  const { addAlert } = useAlerts();
  const { realm } = useRealm();
  const errorHandler = useErrorHandler();

  const [rename, setRename] = useState<string>();

  const history = useHistory();
  const location = useLocation();
  const id = getLastId(location.pathname);

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

  useEffect(
    () =>
      asyncStateFetch(
        async () => {
          const ids = getId(location.pathname);
          const isNavigationStateInValid =
            ids && ids.length !== subGroups.length + 1;
          if (isNavigationStateInValid) {
            const groups: GroupRepresentation[] = [];
            for (const i of ids!) {
              const group = await adminClient.groups.findOne({ id: i });
              if (group) groups.push(group);
            }
            return groups;
          } else {
            if (id) {
              const group = await adminClient.groups.findOne({ id: id });
              if (group) {
                return [...subGroups, group];
              } else {
                return subGroups;
              }
            } else {
              return subGroups;
            }
          }
        },
        (groups: GroupRepresentation[]) => setSubGroups(groups),
        errorHandler
      ),
    [id]
  );

  const SearchDropdown = (
    <DropdownItem
      data-testid="searchGroup"
      key="searchGroup"
      onClick={() => history.push(`/${realm}/groups/search`)}
    >
      {t("searchGroup")}
    </DropdownItem>
  );

  return (
    <>
      {rename && (
        <GroupsModal
          id={id}
          rename={rename}
          refresh={(group) =>
            setSubGroups([...subGroups.slice(0, subGroups.length - 1), group!])
          }
          handleModalToggle={() => setRename(undefined)}
        />
      )}
      <ViewHeader
        titleKey="groups:groups"
        subKey={!id ? "groups:groupsDescription" : ""}
        divider={!id}
        dropdownItems={
          id
            ? [
                SearchDropdown,
                <DropdownItem
                  data-testid="renameGroupAction"
                  key="renameGroup"
                  onClick={() => setRename(currentGroup().name)}
                >
                  {t("renameGroup")}
                </DropdownItem>,
                <DropdownItem
                  data-testid="deleteGroup"
                  key="deleteGroup"
                  onClick={() => {
                    deleteGroup({ id });
                    history.push(
                      location.pathname.substr(
                        0,
                        location.pathname.lastIndexOf("/")
                      )
                    );
                  }}
                >
                  {t("deleteGroup")}
                </DropdownItem>,
              ]
            : [SearchDropdown]
        }
      />
      <PageSection variant={PageSectionVariants.light} className="pf-u-p-0">
        {subGroups.length > 0 && (
          <Tabs
            inset={{
              default: "insetNone",
              md: "insetSm",
              xl: "inset2xl",
              "2xl": "insetLg",
            }}
            activeKey={activeTab}
            onSelect={(_, key) => setActiveTab(key as number)}
            isBox
          >
            <Tab
              data-testid="groups"
              eventKey={0}
              title={<TabTitleText>{t("childGroups")}</TabTitleText>}
            >
              <GroupTable />
            </Tab>
            <Tab
              data-testid="members"
              eventKey={1}
              title={<TabTitleText>{t("members")}</TabTitleText>}
            >
              <Members />
            </Tab>
            <Tab
              data-testid="attributes"
              eventKey={2}
              title={<TabTitleText>{t("attributes")}</TabTitleText>}
            >
              <GroupAttributes />
            </Tab>
          </Tabs>
        )}
        {subGroups.length === 0 && <GroupTable />}
      </PageSection>
    </>
  );
};
