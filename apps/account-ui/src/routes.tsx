import { lazy } from "react";
import type { IndexRouteObject, RouteObject } from "react-router-dom";

import { ErrorPage } from "./root/ErrorPage";
import { Root } from "./root/Root";
import { RootIndex } from "./root/RootIndex";

const DeviceActivity = lazy(() => import("./account-security/DeviceActivity"));
const LinkedAccounts = lazy(() => import("./account-security/LinkedAccounts"));
const SigningIn = lazy(() => import("./account-security/SigningIn"));
const Applications = lazy(() => import("./applications/Applications"));
const Groups = lazy(() => import("./groups/Groups"));
const PersonalInfo = lazy(() => import("./personal-info/PersonalInfo"));
const Resources = lazy(() => import("./resources/Resources"));

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

export const ApplicationsRoute: RouteObject = {
  path: "applications",
  element: <Applications />,
};

export const GroupsRoute: RouteObject = {
  path: "groups",
  element: <Groups />,
};

export const PersonalInfoRoute: RouteObject = {
  path: "personal-info",
  element: <PersonalInfo />,
};

export const ResourcesRoute: RouteObject = {
  path: "resources",
  element: <Resources />,
};

export const RootIndexRoute: IndexRouteObject = {
  index: true,
  element: <RootIndex />,
};

export const RootRoute: RouteObject = {
  path: "/",
  element: <Root />,
  errorElement: <ErrorPage />,
  children: [
    RootIndexRoute,
    DeviceActivityRoute,
    LinkedAccountsRoute,
    SigningInRoute,
    ApplicationsRoute,
    GroupsRoute,
    PersonalInfoRoute,
    ResourcesRoute,
  ],
};

export const routes: RouteObject[] = [RootRoute];
