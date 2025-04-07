import { lazy } from "react";
import type { IndexRouteObject, RouteObject } from "react-router-dom";
import { environment } from "./environment";
import { Organizations } from "./organizations/Organizations";

const DeviceActivity = lazy(() => import("./account-security/DeviceActivity"));
const LinkedAccounts = lazy(() => import("./account-security/LinkedAccounts"));
const SigningIn = lazy(() => import("./account-security/SigningIn"));
const Applications = lazy(() => import("./applications/Applications"));
const Groups = lazy(() => import("./groups/Groups"));
const PersonalInfo = lazy(() => import("./personal-info/PersonalInfo"));
const Resources = lazy(() => import("./resources/Resources"));
const ContentComponent = lazy(() => import("./content/ContentComponent"));
const Oid4Vci = lazy(() => import("./oid4vci/Oid4Vci"));

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

export const Oid4VciRoute: RouteObject = {
  path: "oid4vci",
  element: <Oid4Vci />,
};

export const routes: RouteObject[] = [
  PersonalInfoRoute,
  DeviceActivityRoute,
  LinkedAccountsRoute,
  SigningInRoute,
  ApplicationsRoute,
  GroupsRoute,
  OrganizationsRoute,
  PersonalInfoRoute,
  ResourcesRoute,
  ContentRoute,
  ...(environment.features.isOid4VciEnabled ? [Oid4VciRoute] : []),
];
