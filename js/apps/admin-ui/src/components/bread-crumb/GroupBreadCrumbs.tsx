import { useEffect } from "react";
import { Link, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Breadcrumb, BreadcrumbItem } from "@patternfly/react-core";

import { useAccess } from "../../context/access/Access";
import { useSubGroups } from "../../groups/SubGroupsContext";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useGroupResource } from "../../context/group-resource/GroupResourceContext";

import "../../groups/components/group-tree.css";

export const GroupBreadCrumbs = () => {
  const { t } = useTranslation();
  const { clear, remove, subGroups } = useSubGroups();
  const { realm } = useRealm();
  const { hasAccess } = useAccess();
  const location = useLocation();
  const canViewDetails =
    hasAccess("query-groups", "view-users") ||
    hasAccess("manage-users", "query-groups");

  const isOrgGroups = useGroupResource().isOrgGroups();
  const orgId = useGroupResource().getOrgId();
  const homePath = isOrgGroups
    ? `/${realm}/organizations/${orgId}/groups`
    : `/${realm}/groups`;

  useEffect(() => {
    const { pathname } = location;

    if (!pathname.includes("/groups") || pathname.endsWith("/groups")) {
      clear();
    }
  }, [location]);

  return subGroups.length !== 0 ? (
    <Breadcrumb>
      <BreadcrumbItem key="home">
        <Link to={homePath}>{t("groups")}</Link>
      </BreadcrumbItem>
      {subGroups.map((group, i) => {
        const isLastGroup = i === subGroups.length - 1;
        const canView = isOrgGroups || canViewDetails || group.access?.view;
        return (
          <BreadcrumbItem key={group.id} isActive={isLastGroup}>
            {!isLastGroup && canView && (
              <Link
                to={location.pathname.substring(
                  0,
                  location.pathname.indexOf(group.id!) + group.id!.length,
                )}
                onClick={() => remove(group)}
              >
                {group.name}
              </Link>
            )}
            {!isLastGroup && !canView && (
              <span className="keycloak-groups-tree__non-viewable">
                {group.name}
              </span>
            )}
            {isLastGroup &&
              (group.id === "search" ? group.name : t("groupDetails"))}
          </BreadcrumbItem>
        );
      })}
    </Breadcrumb>
  ) : null;
};
