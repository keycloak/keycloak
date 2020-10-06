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
import { RealmContextProvider } from "../context/realm-context/RealmContext";

export default {
  title: "Header",
  component: RealmSelector,
} as Meta;

export const Header = () => {
  return (
    <RealmContextProvider>
      <Page
        sidebar={
          <PageSidebar
            nav={
              <Nav>
                <NavList>
                  <RealmSelector
                    realmList={[
                      { id: "master", realm: "Master" },
                      { id: "photoz", realm: "Photoz" },
                    ]}
                  />

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
    </RealmContextProvider>
  );
};
