import {
  ErrorPage,
  useEnvironment,
  KeycloakContext,
} from "@keycloak/keycloak-ui-shared";
import { Page, Spinner } from "@patternfly/react-core";
import { Suspense, useState } from "react";
import {
  createBrowserRouter,
  Outlet,
  RouteObject,
  RouterProvider,
} from "react-router-dom";
import fetchContentJson from "../content/fetchContent";
import { Environment, environment } from "../environment";
import { usePromise } from "../utils/usePromise";
import { Header } from "./Header";
import { MenuItem, PageNav } from "./PageNav";
import { routes } from "../routes";

function mapRoutes(
  context: KeycloakContext<Environment>,
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

export const Root = () => {
  const context = useEnvironment<Environment>();
  const [content, setContent] = useState<RouteObject[]>();

  usePromise(
    (signal) => fetchContentJson({ signal, context }),
    (content) => {
      setContent([
        {
          path: decodeURIComponent(new URL(environment.baseUrl).pathname),
          element: (
            <Page header={<Header />} sidebar={<PageNav />} isManagedSidebar>
              <Suspense fallback={<Spinner />}>
                <Outlet />
              </Suspense>
            </Page>
          ),
          errorElement: <ErrorPage />,
          children: mapRoutes(context, content),
        },
      ]);
    },
  );

  if (!content) {
    return <Spinner />;
  }
  return <RouterProvider router={createBrowserRouter(content)} />;
};
