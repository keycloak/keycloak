import { lazy } from "react";
import type { IndexRouteObject, RouteObject } from "react-router-dom";
import { Organizations } from "./organizations/Organizations";

const DeviceActivity = lazy(() => import("./account-security/DeviceActivity"));
const LinkedAccounts = lazy(() => import("./account-security/LinkedAccounts"));
const SigningIn = lazy(() => import("./account-security/SigningIn"));
const Applications = lazy(() => import("./applications/Applications"));
const Groups = lazy(() => import("./groups/Groups"));
const PersonalInfo = lazy(() => import("./personal-info/PersonalInfo"));
const Resources = lazy(() => import("./resources/Resources"));
const VerifiableCredentials = lazy(
  () => import("./verifiable-credentials/VerifiableCredentials"),
);
const ContentComponent = lazy(() => import("./content/ContentComponent"));

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

export const VerifiableCredentialsRoute: RouteObject = {
  path: "verifiable-credentials",
  element: <VerifiableCredentials />,
};

export const ApplicationsRoute: RouteObject = {
  path: "applications",
  element: <Applications />,
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
  path: "content/:componentId",
  element: <ContentComponent />,
};

export const PersonalInfoRoute: IndexRouteObject = {
  index: true,
  element: <PersonalInfo />,
  path: "",
};

export const OrganizationsRoute: RouteObject = {
  path: "organizations",
  element: <Organizations />,
};

export const routes: RouteObject[] = [
  PersonalInfoRoute,
  DeviceActivityRoute,
  LinkedAccountsRoute,
  SigningInRoute,
  VerifiableCredentialsRoute,
  ApplicationsRoute,
  GroupsRoute,
  OrganizationsRoute,
  ResourcesRoute,
  ContentRoute,
];
