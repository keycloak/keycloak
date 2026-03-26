import { Breadcrumb, BreadcrumbItem } from "@patternfly/react-core";
import { uniqBy } from "lodash-es";
import { useTranslation } from "react-i18next";
import { Link, useMatches } from "react-router-dom";

import type { AppRouteObjectHandle } from "../../routes";
import { useRealm } from "../../context/realm-context/RealmContext";

export const PageBreadCrumbs = () => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const matches = useMatches();

  const excludePaths = new Set(["/", `/${realm}`, `/${realm}/page-section`]);

  const crumbs = uniqBy(
    matches
      .filter((match) => !excludePaths.has(match.pathname))
      .map((match) => {
        const handle = match.handle as AppRouteObjectHandle | undefined;
        const breadcrumb = handle?.breadcrumb;
        if (!breadcrumb) return null;
        const label = breadcrumb(t);
        return typeof label === "string"
          ? { pathname: match.pathname, label }
          : null;
      })
      .filter(
        (crumb): crumb is { pathname: string; label: string } => crumb !== null,
      ),
    (crumb) => crumb.label,
  );

  return crumbs.length > 1 ? (
    <Breadcrumb>
      {crumbs.map(({ pathname, label }, i) => (
        <BreadcrumbItem key={i} isActive={crumbs.length - 1 === i}>
          {crumbs.length - 1 !== i ? <Link to={pathname}>{label}</Link> : label}
        </BreadcrumbItem>
      ))}
    </Breadcrumb>
  ) : null;
};
