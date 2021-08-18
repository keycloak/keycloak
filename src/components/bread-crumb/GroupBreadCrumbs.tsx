import React, { useEffect } from "react";
import { Link, useHistory, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Breadcrumb, BreadcrumbItem } from "@patternfly/react-core";

import { useSubGroups } from "../../groups/SubGroupsContext";
import { useRealm } from "../../context/realm-context/RealmContext";

export const GroupBreadCrumbs = () => {
  const { t } = useTranslation();
  const { clear, remove, subGroups } = useSubGroups();
  const { realm } = useRealm();

  const history = useHistory();
  const location = useLocation();

  useEffect(() => {
    return history.listen(({ pathname }) => {
      if (!pathname.includes("/groups") || pathname.endsWith("/groups")) {
        clear();
      }
    });
  }, [history]);

  return (
    <>
      {subGroups.length !== 0 && (
        <Breadcrumb>
          <BreadcrumbItem key="home">
            <Link to={`/${realm}/groups`}>{t("groups")}</Link>
          </BreadcrumbItem>
          {subGroups.map((group, i) => {
            const isLastGroup = i === subGroups.length - 1;
            return (
              <BreadcrumbItem key={group.id} isActive={isLastGroup}>
                {!isLastGroup && (
                  <Link
                    to={location.pathname.substr(
                      0,
                      location.pathname.indexOf(group.id!) + group.id!.length
                    )}
                    onClick={() => remove(group)}
                  >
                    {group.name}
                  </Link>
                )}
                {isLastGroup &&
                  (group.id === "search"
                    ? group.name
                    : t("groups:groupDetails"))}
              </BreadcrumbItem>
            );
          })}
        </Breadcrumb>
      )}
    </>
  );
};
