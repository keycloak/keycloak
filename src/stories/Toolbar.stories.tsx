import React from "react";
import {
  Nav,
  NavItem,
  NavList,
  PageSidebar,
  Page,
} from "@patternfly/react-core";
import { Meta } from "@storybook/react";

import { RealmSelector } from "../components/realm-selector/RealmSelector";
import { RealmContext } from "../context/realm-context/RealmContext";
import { HashRouter } from "react-router-dom";

export default {
  title: "Header",
  component: RealmSelector,
} as Meta;

export const Header = () => {
  return (
    <HashRouter>
      <RealmContext.Provider
        value={{
          realm: "master",
          setRealm: () => {},
          realms: [
            { id: "master", realm: "Master" },
            { id: "photoz", realm: "Photoz" },
          ],
          refresh: () => Promise.resolve(),
        }}
      >
        <Page
          sidebar={
            <PageSidebar
              nav={
                <Nav>
                  <NavList>
                    <RealmSelector />
                    <NavItem id="default-link1" to="#default-link1" itemId={0}>
                      Link 1
                    </NavItem>
                    <NavItem
                      id="default-link2"
                      to="#default-link2"
                      itemId={1}
                      isActive
                    >
                      Current link
                    </NavItem>
                    <NavItem id="default-link3" to="#default-link3" itemId={2}>
                      Link 3
                    </NavItem>
                    <NavItem id="default-link4" to="#default-link4" itemId={3}>
                      Link 4
                    </NavItem>
                  </NavList>
                </Nav>
              }
            />
          }
        />
      </RealmContext.Provider>
    </HashRouter>
  );
};
