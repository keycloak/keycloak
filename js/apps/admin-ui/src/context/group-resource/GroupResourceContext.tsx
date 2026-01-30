import {
  createNamedContext,
  useRequiredContext,
} from "@keycloak/keycloak-ui-shared";
import { Groups } from "@keycloak/keycloak-admin-client";
import { PropsWithChildren } from "react";

export const GroupsResourceContext = createNamedContext<Groups | undefined>(
  "GroupResourceContest",
  undefined,
);

export const useGroupResource = () => useRequiredContext(GroupsResourceContext);

type GroupsContextProps = PropsWithChildren & {
  groupResource: Groups;
};
export const GroupResourceContext = ({
  groupResource,
  children,
}: GroupsContextProps) => {
  return (
    <GroupsResourceContext.Provider value={groupResource}>
      {children}
    </GroupsResourceContext.Provider>
  );
};
