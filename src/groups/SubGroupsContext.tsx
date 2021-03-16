import React, { createContext, ReactNode, useContext, useState } from "react";
import GroupRepresentation from "keycloak-admin/lib/defs/groupRepresentation";

type SubGroupsProps = {
  subGroups: GroupRepresentation[];
  setSubGroups: (group: GroupRepresentation[]) => void;
  clear: () => void;
  remove: (group: GroupRepresentation) => void;
  currentGroup: () => GroupRepresentation;
};

const SubGroupContext = createContext<SubGroupsProps>({
  subGroups: [],
  setSubGroups: () => {},
  clear: () => {},
  remove: () => {},
  currentGroup: () => {
    return {};
  },
});

export const SubGroups = ({ children }: { children: ReactNode }) => {
  const [subGroups, setSubGroups] = useState<GroupRepresentation[]>([]);

  const clear = () => setSubGroups([]);
  const remove = (group: GroupRepresentation) =>
    setSubGroups(
      subGroups.slice(
        0,
        subGroups.findIndex((g) => g.id === group.id)
      )
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

export const useSubGroups = () => useContext(SubGroupContext);
