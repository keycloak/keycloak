import type GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";
import React, { createContext, ReactNode, useState } from "react";
import useRequiredContext from "../utils/useRequiredContext";

type SubGroupsProps = {
  subGroups: GroupRepresentation[];
  setSubGroups: (group: GroupRepresentation[]) => void;
  clear: () => void;
  remove: (group: GroupRepresentation) => void;
  currentGroup: () => GroupRepresentation;
};

const SubGroupContext = createContext<SubGroupsProps | undefined>(undefined);

export const SubGroups = ({ children }: { children: ReactNode }) => {
  const [subGroups, setSubGroups] = useState<GroupRepresentation[]>([]);

  const clear = () => setSubGroups([]);
  const remove = (group: GroupRepresentation) =>
    setSubGroups(
      subGroups.slice(0, subGroups.findIndex((g) => g.id === group.id) + 1)
    );
  const currentGroup = () => subGroups[subGroups.length - 1];
  return (
    <SubGroupContext.Provider
      value={{ subGroups, setSubGroups, clear, remove, currentGroup }}
    >
      {children}
    </SubGroupContext.Provider>
  );
};

export const useSubGroups = () => useRequiredContext(SubGroupContext);
