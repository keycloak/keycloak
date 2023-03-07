import {
  Checkbox,
  DataList,
  DataListCell,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { getGroups } from "../api/methods";
import { Group } from "../api/representations";
import { Page } from "../components/page/Page";
import { usePromise } from "../utils/usePromise";

const Groups = () => {
  const { t } = useTranslation();

  const [groups, setGroups] = useState<Group[]>([]);
  const [directMembership, setDirectMembership] = useState(false);

  usePromise(
    (signal) => getGroups({ signal }),
    (groups) => {
      if (directMembership) {
        groups.forEach((el) =>
          getParents(
            el,
            groups,
            groups.map(({ path }) => path)
          )
        );
      }
      setGroups(groups);
    },
    [directMembership]
  );

  const getParents = (el: Group, groups: Group[], groupsPaths: string[]) => {
    const parentPath = el.path.slice(0, el.path.lastIndexOf("/"));
    if (parentPath && !groupsPaths.includes(parentPath)) {
      el = {
        name: parentPath.slice(parentPath.lastIndexOf("/") + 1),
        path: parentPath,
      };
      groups.push(el);
      groupsPaths.push(parentPath);

      getParents(el, groups, groupsPaths);
    }
  };

  return (
    <Page title={t("groupLabel")} description={t("groupDescriptionLabel")}>
      <DataList id="groups-list" aria-label={t("groupLabel")} isCompact>
        <DataListItem id="groups-list-header" aria-labelledby="Columns names">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell key="directMembership-header">
                  <Checkbox
                    label={t("directMembership")}
                    id="directMembership-checkbox"
                    isChecked={directMembership}
                    onChange={(checked) => setDirectMembership(checked)}
                  />
                </DataListCell>,
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        <DataListItem id="groups-list-header" aria-labelledby="Columns names">
          <DataListItemRow>
            <DataListItemCells
              dataListCells={[
                <DataListCell key="group-name-header" width={2}>
                  <strong>{t("name")}</strong>
                </DataListCell>,
                <DataListCell key="group-path-header" width={2}>
                  <strong>{t("path")}</strong>
                </DataListCell>,
                <DataListCell key="group-direct-membership-header" width={2}>
                  <strong>{t("directMembership")}</strong>
                </DataListCell>,
              ]}
            />
          </DataListItemRow>
        </DataListItem>
        {groups.map((group, appIndex) => (
          <DataListItem
            id={`${appIndex}-group`}
            key={"group-" + appIndex}
            aria-labelledby="groups-list"
          >
            <DataListItemRow>
              <DataListItemCells
                dataListCells={[
                  <DataListCell
                    id={`${appIndex}-group-name`}
                    width={2}
                    key={"name-" + appIndex}
                  >
                    {group.name}
                  </DataListCell>,
                  <DataListCell
                    id={`${appIndex}-group-path`}
                    width={2}
                    key={"path-" + appIndex}
                  >
                    {group.path}
                  </DataListCell>,
                  <DataListCell
                    id={`${appIndex}-group-directMembership`}
                    width={2}
                    key={"directMembership-" + appIndex}
                  >
                    <Checkbox
                      id={`${appIndex}-checkbox-directMembership`}
                      isChecked={group.id != null}
                      isDisabled={true}
                    />
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
        ))}
      </DataList>
    </Page>
  );
};

export default Groups;
