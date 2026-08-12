import {
  createNamedContext,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { Organizations } from "@keycloak/keycloak-admin-client/lib/resources/organizations";
import { PropsWithChildren } from "react";

export const OrganizationsResourceContext = createNamedContext<
  Organizations | undefined
>("OrganizationsResourceContext", undefined);

export const useOrganizationResource = () =>
  useRequiredContext(OrganizationsResourceContext);

type OrganizationsContextProps = PropsWithChildren & {
  value: Organizations;
};
export const OrganizationResourceContext = ({
  value,
  children,
}: OrganizationsContextProps) => {
  return (
    <OrganizationsResourceContext.Provider value={value}>
      {children}
    </OrganizationsResourceContext.Provider>
  );
};
