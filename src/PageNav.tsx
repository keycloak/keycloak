import React, { useContext } from "react";
import { Nav, NavItem, NavList, PageSidebar } from "@patternfly/react-core";
import { RealmSelector } from "./components/realm-selector/RealmSelector";
import { DataLoader } from "./components/data-loader/DataLoader";
import { HttpClientContext } from "./http-service/HttpClientContext";
import { Realm } from "./models/Realm";

export const PageNav: React.FunctionComponent = () => {
  const httpClient = useContext(HttpClientContext)!;
  const realmLoader = async () => {
    const response = await httpClient.doGet<Realm[]>("/admin/realms");
    return response.data;
  };
  return (
    <PageSidebar
      nav={
        <Nav>
          <NavList>
            <DataLoader loader={realmLoader}>
              {(realmList) => (
                <RealmSelector realm="Master" realmList={realmList || []} />
              )}
            </DataLoader>
            <NavItem id="default-link1" to="/default-link1" itemId={0}>
              Link 1
            </NavItem>
            <NavItem id="default-link2" to="/default-link2" itemId={1} isActive>
              Current link
            </NavItem>
            <NavItem id="default-link3" to="/default-link3" itemId={2}>
              Link 3
            </NavItem>
            <NavItem id="default-link4" to="/default-link4" itemId={3}>
              Link 4
            </NavItem>
          </NavList>
        </Nav>
      }
    />
  );
};
