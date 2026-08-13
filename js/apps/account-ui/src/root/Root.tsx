import {
  ErrorPage,
  useAlerts,
  useEnvironment,
  KeycloakContext,
} from "@keycloak/keycloak-ui-shared";
import { AlertVariant, Page, Spinner } from "@patternfly/react-core";
import { Suspense, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  createBrowserRouter,
  Navigate,
  Outlet,
  RouteObject,
  RouterProvider,
} from "react-router-dom";
import fetchContentJson from "../content/fetchContent";
import { type AccountEnvironment } from "..";
import { usePromise } from "../utils/usePromise";
import { Header } from "./Header";
import { MenuItem, PageNav } from "./PageNav";
import { routes } from "../routes";

function mapRoutes(
  context: KeycloakContext<AccountEnvironment>,
  content: MenuItem[],
): RouteObject[] {
  return content
    .map((item) => {
      if ("children" in item) {
        return mapRoutes(context, item.children);
      }

      // Do not add route disabled via feature flags
      if (item.isVisible && !context.environment.features[item.isVisible]) {
        return null;
      }

      return {
        ...item,
        element:
          "path" in item
            ? routes.find((r) => r.path === (item.id ?? item.path))?.element
            : undefined,
      };
    })
    .filter((item) => !!item)
    .flat();
}

function CatchAllRedirect() {
  const { t } = useTranslation();
  const { addAlert } = useAlerts();

  useEffect(() => {
    addAlert(t("pageNotFound"), AlertVariant.warning);
  }, [addAlert, t]);

  return <Navigate to="." replace />;
}

export const Root = () => {
  const context = useEnvironment<AccountEnvironment>();
  const [content, setContent] = useState<RouteObject[]>();

  usePromise(
    (signal) => fetchContentJson({ signal, context }),
    (content) => {
      setContent([
        {
          path: decodeURIComponent(
            new URL(context.environment.baseUrl).pathname,
          ),
          element: (
            <Page header={<Header />} sidebar={<PageNav />} isManagedSidebar>
              <Suspense fallback={<Spinner />}>
                <Outlet />
              </Suspense>
            </Page>
          ),
          errorElement: <ErrorPage />,
          children: [
            ...mapRoutes(context, content),
            { path: "*", element: <CatchAllRedirect /> },
          ],
        },
      ]);
    },
  );

  if (!content) {
    return <Spinner />;
  }
  return <RouterProvider router={createBrowserRouter(content)} />;
};
