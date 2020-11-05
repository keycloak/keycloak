import React from "react";
import { Link } from "react-router-dom";
import useBreadcrumbs from "use-react-router-breadcrumbs";
import { useTranslation } from "react-i18next";
import { Breadcrumb, BreadcrumbItem } from "@patternfly/react-core";

import { routes } from "../../route-config";

export const PageBreadCrumbs = () => {
  const { t } = useTranslation();
  const crumbs = useBreadcrumbs(routes(t)).slice(1);
  return (
    <>
      {crumbs.length > 1 && (
        <Breadcrumb>
          {crumbs.map(({ match, breadcrumb: crumb }, i) => (
            <BreadcrumbItem key={i} isActive={crumbs.length - 1 === i}>
              {crumbs.length - 1 !== i && <Link to={match.url}>{crumb}</Link>}
              {crumbs.length - 1 === i && <>{crumb}</>}
            </BreadcrumbItem>
          ))}
        </Breadcrumb>
      )}
    </>
  );
};
