import React, { useEffect } from "react";
import { mount } from "enzyme";
import { MemoryRouter } from "react-router-dom";

import { GroupBreadCrumbs } from "../GroupBreadCrumbs";
import { SubGroups, useSubGroups } from "../../../groups/SubGroupsContext";

const GroupCrumbs = () => {
  const { setSubGroups } = useSubGroups();
  useEffect(() => {
    setSubGroups([
      { id: "1", name: "first group" },
      { id: "123", name: "active group" },
    ]);
  }, []);

  return <GroupBreadCrumbs />;
};

describe("Group BreadCrumbs tests", () => {
  it("couple of crumbs", () => {
    const crumbs = mount(
      <MemoryRouter initialEntries={["/groups"]}>
        <SubGroups>
          <GroupCrumbs />
        </SubGroups>
      </MemoryRouter>
    );

    expect(crumbs.find(GroupCrumbs)).toMatchSnapshot();
  });
});
