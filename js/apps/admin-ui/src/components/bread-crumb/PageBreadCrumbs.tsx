import { Breadcrumb, BreadcrumbItem } from "@patternfly/react-core";
import { uniqBy } from "lodash-es";
import { isValidElement } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import useBreadcrumbs, {
  BreadcrumbData,
  BreadcrumbsRoute,
} from "use-react-router-breadcrumbs";

import { useRealm } from "../../context/realm-context/RealmContext";
import { routes } from "../../routes";

export const PageBreadCrumbs = () => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const elementText = (crumb: BreadcrumbData) =>
    isValidElement(crumb.breadcrumb) && crumb.breadcrumb.props.children;

  const routesWithCrumbs: BreadcrumbsRoute[] = routes.map((route) => ({
    ...route,
    breadcrumb: route.breadcrumb?.(t),
  }));

  const crumbs = uniqBy(
    useBreadcrumbs(routesWithCrumbs, {
      disableDefaults: true,
      excludePaths: ["/", `/${realm}`, `/${realm}/page-section`],
    }),
    elementText,
  );
  return crumbs.length > 1 ? (
    <Breadcrumb>
      {crumbs.map(({ match, breadcrumb: crumb }, i) => (
        <BreadcrumbItem key={i} isActive={crumbs.length - 1 === i}>
          {crumbs.length - 1 !== i ? (
            <Link to={match.pathname}>{crumb}</Link>
          ) : (
            crumb
          )}
        </BreadcrumbItem>
      ))}
    </Breadcrumb>
  ) : null;
};
