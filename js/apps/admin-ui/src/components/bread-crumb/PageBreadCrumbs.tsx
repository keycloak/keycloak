import { Breadcrumb, BreadcrumbItem } from "@patternfly/react-core";
import { uniqBy } from "lodash-es";
import { useTranslation } from "react-i18next";
import { Link, matchPath, useLocation } from "react-router-dom";

import { useRealm } from "../../context/realm-context/RealmContext";
import { routes } from "../../routes";

/** Count literal (non-parameterized) segments so we can prefer specific routes. */
const specificity = (path: string) =>
  path.split("/").filter((s) => s && !s.startsWith(":")).length;

export const PageBreadCrumbs = () => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { pathname } = useLocation();

  const excludePaths = new Set(["/", `/${realm}`, `/${realm}/page-section`]);

  // Find every route whose pattern is a prefix of the current URL.
  type Crumb = {
    pathname: string;
    label: string;
    pathLength: number;
    specificity: number;
  };

  const matched: Crumb[] = [];

  for (const route of routes) {
    const match = matchPath({ path: route.path, end: false }, pathname);
    if (!match || excludePaths.has(match.pathname)) continue;

    const breadcrumb = route.handle.breadcrumb;
    if (!breadcrumb) continue;

    const label = breadcrumb(t);
    if (typeof label !== "string") continue;

    matched.push({
      pathname: match.pathname,
      label,
      pathLength: match.pathname.length,
      specificity: specificity(route.path),
    });
  }

  // When multiple routes resolve to the same pathname keep the most specific.
  const best = new Map<string, Crumb>();
  for (const crumb of matched) {
    const prev = best.get(crumb.pathname);
    if (!prev || crumb.specificity > prev.specificity) {
      best.set(crumb.pathname, crumb);
    }
  }

  const crumbs = uniqBy(
    [...best.values()].sort((a, b) => a.pathLength - b.pathLength),
    (c) => c.label,
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
