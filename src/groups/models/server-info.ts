export interface ServerGroupsRepresentation {
  id?: number;
  name?: string;
  path?: string;
  subGroups?: [];
}

// TO DO: Update this to represent the data that is returned
export interface ServerGroupMembersRepresentation {
  data?: [];
}

export interface ServerGroupsArrayRepresentation {
  groups: { [index: string]: ServerGroupsRepresentation[] };
}
