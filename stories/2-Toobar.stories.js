import React, { useContext } from 'react';
import { storiesOf } from '@storybook/react';
import { Nav, NavItem, NavList, PageSidebar, Page, PageHeader, PageHeaderTools, PageHeaderToolsItem } from '@patternfly/react-core';

import { RealmSelector } from '../src/components/realm-selector/RealmSelector';
import { Help, HelpContext, HelpHeader } from '../src/components/help-enabler/HelpHeader';

storiesOf('Toolbar')
  .add('realm selector', () => {
    return (
      <Page sidebar={
        <PageSidebar nav={
          <Nav>
            <NavList>
              <RealmSelector realm="Master" realmList={["Master", "Photoz"]} />

              <NavItem id="default-link1" to="#default-link1" itemId={0}>
                Link 1
              </NavItem>
              <NavItem id="default-link2" to="#default-link2" itemId={1} isActive>
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
        } />
      } />
    );
  })
  .add('help system', () => {
    return (
      <Help>
        <HelpSystemTest />
      </Help>
    );
  });

const HelpSystemTest = () => {
  const { enabled } = useContext(HelpContext);
  return (
    <Page header={<PageHeader
      headerTools={
        <PageHeaderTools>
          <PageHeaderToolsItem>
            <HelpHeader />
          </PageHeaderToolsItem>
          <PageHeaderToolsItem>
            dummy user...
          </PageHeaderToolsItem>
        </PageHeaderTools>}
    />}>
      Help system is {enabled ? 'enabled' : "not on, guess you don't need help"}
    </Page>
  );
}
