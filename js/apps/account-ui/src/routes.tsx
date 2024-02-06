import { lazy } from "react";
import type { IndexRouteObject, RouteObject } from "react-router-dom";

import { ErrorPage } from "./root/ErrorPage";
import { Root } from "./root/Root";

const DeviceActivity = lazy(() => import("./account-security/DeviceActivity"));
const LinkedAccounts = lazy(() => import("./account-security/LinkedAccounts"));
const SigningIn = lazy(() => import("./account-security/SigningIn"));
const Applications = lazy(() => import("./applications/Applications"));
const Groups = lazy(() => import("./groups/Groups"));
const PersonalInfo = lazy(() => import("./personal-info/PersonalInfo"));
const Resources = lazy(() => import("./resources/Resources"));
const ContentComponent = lazy(() => import("./content/ContentComponent"));

export const ApplicationsRoute: IndexRouteObject = {
  index: true,
  element: <Applications />,
};

export const PersonalInfoRoute: RouteObject = {
  path: "personal-info",
  element: <PersonalInfo />,
};

export const DeviceActivityRoute: RouteObject = {
  path: "account-security/device-activity",
  element: <DeviceActivity />,
};

export const LinkedAccountsRoute: RouteObject = {
  path: "account-security/linked-accounts",
  element: <LinkedAccounts />,
};

export const SigningInRoute: RouteObject = {
  path: "account-security/signing-in",
  element: <SigningIn />,
};

export const GroupsRoute: RouteObject = {
  path: "groups",
  element: <Groups />,
};

export const ResourcesRoute: RouteObject = {
  path: "resources",
  element: <Resources />,
};

export type ContentComponentParams = {
  componentId: string;
};

export const ContentRoute: RouteObject = {
  path: "/content/:componentId",
  element: <ContentComponent />,
};

export const RootRoute: RouteObject = {
  path: "/",
  element: <Root />,
  errorElement: <ErrorPage />,
  children: [
    ApplicationsRoute,
    PersonalInfoRoute,
    DeviceActivityRoute,
    LinkedAccountsRoute,
    SigningInRoute,
    GroupsRoute,
    ResourcesRoute,
    ContentRoute,
  ],
};

export const routes: RouteObject[] = [RootRoute];
