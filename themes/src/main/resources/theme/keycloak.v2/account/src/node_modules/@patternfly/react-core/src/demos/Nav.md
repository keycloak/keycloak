---
id: Navigation
section: components
---

import { DashboardBreadcrumb } from './examples/DashboardWrapper';
import DashboardHeader from './examples/DashboardHeader';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import imgBrand from '@patternfly/react-core/src/components/Brand/examples/pfLogo.svg';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';
import imgColorBrand from '@patternfly/react-core/src/demos/examples/pfColorLogo.svg';

## Demos

### Default nav

```js isFullscreen
import React from 'react';
import {
  Card,
  CardBody,
  Gallery,
  GalleryItem,
  Nav,
  NavItem,
  NavList,
  Page,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  SkipToContent,
  TextContent,
  Text
} from '@patternfly/react-core';
import { DashboardBreadcrumb } from './examples/DashboardWrapper';
import DashboardHeader from './examples/DashboardHeader';

class PageLayoutDefaultNav extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      activeItem: 0
    };

    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId
      });
    };
  }

  render() {
    const { activeItem } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavList>
          <NavItem itemId={0} isActive={activeItem === 0} to="#">
            System Panel
          </NavItem>
          <NavItem itemId={1} isActive={activeItem === 1} to="#">
            Policy
          </NavItem>
          <NavItem itemId={2} isActive={activeItem === 2} to="#">
            Authentication
          </NavItem>
          <NavItem itemId={3} isActive={activeItem === 3} to="#">
            Network Services
          </NavItem>
          <NavItem itemId={4} isActive={activeItem === 4} to="#">
            Server
          </NavItem>
        </NavList>
      </Nav>
    );

    const Sidebar = <PageSidebar nav={PageNav} />;
    const pageId = 'main-content-page-layout-default-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to content</SkipToContent>;

    return (
      <React.Fragment>
        <Page
          header={<DashboardHeader />}
          sidebar={Sidebar}
          isManagedSidebar
          skipToContent={PageSkipToContent}
          breadcrumb={DashboardBreadcrumb}
          mainContainerId={pageId}
        >
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of its relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(10)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Grouped nav

```js isFullscreen
import React from 'react';
import {
  Nav,
  NavGroup,
  NavItem,
  Page,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  SkipToContent,
  TextContent,
  Text
} from '@patternfly/react-core';
import DashboardHeader from './examples/DashboardHeader';

class PageLayoutGroupsNav extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      activeItem: 'grp-1_itm-1'
    };

    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId
      });
    };
  }

  render() {
    const { activeItem } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavGroup title="System Panel">
          <NavItem itemId="grp-1_itm-1" isActive={activeItem === 'grp-1_itm-1'} to="#">
            Overview
          </NavItem>
          <NavItem itemId="grp-1_itm-2" isActive={activeItem === 'grp-1_itm-2'} to="#">
            Resource Usage
          </NavItem>
          <NavItem itemId="grp-1_itm-3" isActive={activeItem === 'grp-1_itm-3'} to="#">
            Hypervisors
          </NavItem>
          <NavItem itemId="grp-1_itm-4" isActive={activeItem === 'grp-1_itm-4'} to="#">
            Instances
          </NavItem>
          <NavItem itemId="grp-1_itm-5" isActive={activeItem === 'grp-1_itm-5'} to="#">
            Volumes
          </NavItem>
          <NavItem itemId="grp-1_itm-6" isActive={activeItem === 'grp-1_itm-6'} to="#">
            Network
          </NavItem>
        </NavGroup>
        <NavGroup title="Policy">
          <NavItem itemId="grp-2_itm-1" isActive={activeItem === 'grp-2_itm-1'} to="#">
            Hosts
          </NavItem>
          <NavItem itemId="grp-2_itm-2" isActive={activeItem === 'grp-2_itm-2'} to="#">
            Virtual Machines
          </NavItem>
          <NavItem itemId="grp-2_itm-3" isActive={activeItem === 'grp-2_itm-3'} to="#">
            Storage
          </NavItem>
        </NavGroup>
      </Nav>
    );

    const Sidebar = <PageSidebar nav={PageNav} />;
    const pageId = 'main-content-page-layout-group-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to Content</SkipToContent>;

    return (
      <React.Fragment>
        <Page
          header={<DashboardHeader />}
          sidebar={Sidebar}
          isManagedSidebar
          skipToContent={PageSkipToContent}
          mainContainerId={pageId}
        >
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of its relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection variant={PageSectionVariants.light}>Light</PageSection>
          <PageSection variant={PageSectionVariants.dark}>Dark</PageSection>
          <PageSection variant={PageSectionVariants.darker}>Darker</PageSection>
          <PageSection>Content</PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Expandable nav

```js isFullscreen
import React from 'react';
import {
  Card,
  CardBody,
  Gallery,
  GalleryItem,
  Nav,
  NavExpandable,
  NavItem,
  NavList,
  Page,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  SkipToContent,
  TextContent,
  Text
} from '@patternfly/react-core';
import { DashboardBreadcrumb } from './examples/DashboardWrapper';
import DashboardHeader from './examples/DashboardHeader';

class PageLayoutExpandableNav extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      activeGroup: 'grp-1',
      activeItem: 'grp-1_itm-1'
    };

    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId,
        activeGroup: result.groupId
      });
    };
  }

  render() {
    const { activeItem, activeGroup } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavList>
          <NavExpandable title="System Panel" groupId="grp-1" isActive={activeGroup === 'grp-1'} isExpanded>
            <NavItem groupId="grp-1" itemId="grp-1_itm-1" isActive={activeItem === 'grp-1_itm-1'} to="#">
              Overview
            </NavItem>
            <NavItem groupId="grp-1" itemId="grp-1_itm-2" isActive={activeItem === 'grp-1_itm-2'} to="#">
              Resource usage
            </NavItem>
            <NavItem groupId="grp-1" itemId="grp-1_itm-3" isActive={activeItem === 'grp-1_itm-3'} to="#">
              Hypervisors
            </NavItem>
            <NavItem groupId="grp-1" itemId="grp-1_itm-4" isActive={activeItem === 'grp-1_itm-4'} to="#">
              Instances
            </NavItem>
            <NavItem groupId="grp-1" itemId="grp-1_itm-5" isActive={activeItem === 'grp-1_itm-5'} to="#">
              Volumes
            </NavItem>
            <NavItem groupId="grp-1" itemId="grp-1_itm-6" isActive={activeItem === 'grp-1_itm-6'} to="#">
              Network
            </NavItem>
          </NavExpandable>
          <NavExpandable title="Policy" groupId="grp-2" isActive={activeGroup === 'grp-2'} to="#">
            <NavItem groupId="grp-2" itemId="grp-2_itm-1" isActive={activeItem === 'grp-2_itm-1'} to="#">
              Subnav link 1
            </NavItem>
            <NavItem groupId="grp-2" itemId="grp-2_itm-2" isActive={activeItem === 'grp-2_itm-2'} to="#">
              Subnav link 2
            </NavItem>
          </NavExpandable>
          <NavExpandable title="Authentication" groupId="grp-3" isActive={activeGroup === 'grp-3'}>
            <NavItem groupId="grp-3" itemId="grp-3_itm-1" isActive={activeItem === 'grp-3_itm-1'} to="#">
              Subnav link 1
            </NavItem>
            <NavItem groupId="grp-3" itemId="grp-3_itm-2" isActive={activeItem === 'grp-3_itm-2'} to="#">
              Subnav link 2
            </NavItem>
          </NavExpandable>
        </NavList>
      </Nav>
    );

    const Sidebar = <PageSidebar nav={PageNav} />;
    const pageId = 'main-content-page-layout-expandable-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to content</SkipToContent>;

    return (
      <React.Fragment>
        <Page
          header={<DashboardHeader />}
          sidebar={Sidebar}
          isManagedSidebar
          skipToContent={PageSkipToContent}
          breadcrumb={DashboardBreadcrumb}
          mainContainerId={pageId}
        >
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of its relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(10)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Horizontal nav

```js isFullscreen
import React from 'react';
import {
  Avatar,
  Brand,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  Dropdown,
  DropdownGroup,
  DropdownToggle,
  DropdownItem,
  Gallery,
  GalleryItem,
  KebabToggle,
  Nav,
  NavItem,
  NavList,
  Page,
  PageHeader,
  PageSection,
  PageSectionVariants,
  SkipToContent,
  TextContent,
  Text,
  PageHeaderTools,
  PageHeaderToolsGroup,
  PageHeaderToolsItem
} from '@patternfly/react-core';
import { DashboardBreadcrumb } from './examples/DashboardWrapper';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import imgBrand from '@patternfly/react-core/src/components/Brand/examples/pfLogo.svg';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';

class PageLayoutHorizontalNav extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isDropdownOpen: false,
      isKebabDropdownOpen: false,
      activeItem: 0
    };

    this.onDropdownToggle = isDropdownOpen => {
      this.setState({
        isDropdownOpen
      });
    };

    this.onDropdownSelect = event => {
      this.setState({
        isDropdownOpen: !this.state.isDropdownOpen
      });
    };

    this.onKebabDropdownToggle = isKebabDropdownOpen => {
      this.setState({
        isKebabDropdownOpen
      });
    };

    this.onKebabDropdownSelect = event => {
      this.setState({
        isKebabDropdownOpen: !this.state.isKebabDropdownOpen
      });
    };

    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId
      });
    };
  }

  render() {
    const { isDropdownOpen, isKebabDropdownOpen, activeItem } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav" variant="horizontal">
        <NavList>
          <NavItem itemId={0} isActive={activeItem === 0} to="#">
            System Panel
          </NavItem>
          <NavItem itemId={1} isActive={activeItem === 1} to="#">
            Policy
          </NavItem>
          <NavItem itemId={2} isActive={activeItem === 2} to="#">
            Authentication
          </NavItem>
          <NavItem itemId={3} isActive={activeItem === 3} to="#">
            Network Services
          </NavItem>
          <NavItem itemId={4} isActive={activeItem === 4} to="#">
            Server
          </NavItem>
        </NavList>
      </Nav>
    );
    const kebabDropdownItems = [
      <DropdownItem>
        <CogIcon /> Settings
      </DropdownItem>,
      <DropdownItem>
        <HelpIcon /> Help
      </DropdownItem>
    ];
    const userDropdownItems = [
      <DropdownGroup key="group 2">
        <DropdownItem key="group 2 profile">My profile</DropdownItem>
        <DropdownItem key="group 2 user" component="button">
          User management
        </DropdownItem>
        <DropdownItem key="group 2 logout">Logout</DropdownItem>
      </DropdownGroup>
    ];
    const headerTools = (
      <PageHeaderTools>
        <PageHeaderToolsGroup
          visibility={{
            default: 'hidden',
            lg: 'visible'
          }} /** the settings and help icon buttons are only visible on desktop sizes and replaced by a kebab dropdown for other sizes */
        >
          <PageHeaderToolsItem>
            <Button aria-label="Settings actions" variant={ButtonVariant.plain}>
              <CogIcon />
            </Button>
          </PageHeaderToolsItem>
          <PageHeaderToolsItem>
            <Button aria-label="Help actions" variant={ButtonVariant.plain}>
              <QuestionCircleIcon />
            </Button>
          </PageHeaderToolsItem>
        </PageHeaderToolsGroup>
        <PageHeaderToolsGroup>
          <PageHeaderToolsItem
            visibility={{
              lg: 'hidden'
            }} /** this kebab dropdown replaces the icon buttons and is hidden for desktop sizes */
          >
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onKebabDropdownSelect}
              toggle={<KebabToggle onToggle={this.onKebabDropdownToggle} />}
              isOpen={isKebabDropdownOpen}
              dropdownItems={kebabDropdownItems}
            />
          </PageHeaderToolsItem>
          <PageHeaderToolsItem
            visibility={{ default: 'hidden', md: 'visible' }} /** this user dropdown is hidden on mobile sizes */
          >
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onDropdownSelect}
              isOpen={isDropdownOpen}
              toggle={<DropdownToggle onToggle={this.onDropdownToggle}>John Smith</DropdownToggle>}
              dropdownItems={userDropdownItems}
            />
          </PageHeaderToolsItem>
        </PageHeaderToolsGroup>
        <Avatar src={imgAvatar} alt="Avatar image" />
      </PageHeaderTools>
    );

    const Header = (
      <PageHeader logo={<Brand src={imgBrand} alt="Patternfly Logo" />} headerTools={headerTools} topNav={PageNav} />
    );

    const pageId = 'main-content-page-layout-horizontal-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to content</SkipToContent>;

    return (
      <React.Fragment>
        <Page
          header={Header}
          skipToContent={PageSkipToContent}
          breadcrumb={DashboardBreadcrumb}
          mainContainerId={pageId}
        >
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of its relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(10)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Horizontal subnav

```js isFullscreen
import React from 'react';
import {
  Card,
  CardBody,
  Gallery,
  GalleryItem,
  Nav,
  NavItem,
  NavList,
  Page,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  SkipToContent,
  TextContent,
  Text
} from '@patternfly/react-core';
import { DashboardBreadcrumb } from './examples/DashboardWrapper';
import DashboardHeader from './examples/DashboardHeader';

class VerticalNavWithSubnav extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      activeItem: 0,
      activeSubNavItem: 7
    };

    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId
      });
    };

    this.onSubNavSelect = result => {
      this.setState({
        activeSubNavItem: result.itemId
      });
    };
  }

  render() {
    const { activeItem, activeSubNavItem } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavList>
          <NavItem itemId={0} isActive={activeItem === 0} to="#">
            System Panel
          </NavItem>
          <NavItem itemId={1} isActive={activeItem === 1} to="#">
            Policy
          </NavItem>
          <NavItem itemId={2} isActive={activeItem === 2} to="#">
            Authentication
          </NavItem>
          <NavItem itemId={3} isActive={activeItem === 3} to="#">
            Network Services
          </NavItem>
          <NavItem itemId={4} isActive={activeItem === 4} to="#">
            Server
          </NavItem>
        </NavList>
      </Nav>
    );

    const Sidebar = <PageSidebar nav={PageNav} />;
    const pageId = 'main-content-page-layout-default-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to content</SkipToContent>;

    const SubNav = (
      <Nav onSelect={this.onSubNavSelect} aria-label="Nav" variant="horizontal-subnav">
        <NavList>
          <NavItem itemId={7} isActive={activeSubNavItem === 7} to="#">
            Horizontal subnav item 1
          </NavItem>
          <NavItem itemId={8} isActive={activeSubNavItem === 8} to="#">
            Horizontal subnav item 2
          </NavItem>
          <NavItem itemId={9} isActive={activeSubNavItem === 9} to="#">
            Horizontal subnav item 3
          </NavItem>
          <NavItem itemId={10} isActive={activeSubNavItem === 10} to="#">
            Horizontal subnav item 4
          </NavItem>
          <NavItem itemId={11} isActive={activeSubNavItem === 11} to="#">
            Horizontal subnav item 5
          </NavItem>
          <NavItem itemId={12} isActive={activeSubNavItem === 12} to="#">
            Horizontal subnav item 6
          </NavItem>
          <NavItem itemId={13} isActive={activeSubNavItem === 13} to="#">
            Horizontal subnav item 7
          </NavItem>
        </NavList>
      </Nav>
    );

    return (
      <React.Fragment>
        <Page
          header={<DashboardHeader />}
          sidebar={Sidebar}
          isManagedSidebar
          skipToContent={PageSkipToContent}
          mainContainerId={pageId}
        >
          <PageSection type={PageSectionTypes.subNav} isWidthLimited>
            {SubNav}
          </PageSection>
          <PageSection type={PageSectionTypes.breadcrumb} isWidthLimited>
            {DashboardBreadcrumb}
          </PageSection>
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of it’s relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(10)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Horizontal nav with horizontal subnav

```js isFullscreen
import React from 'react';
import {
  Avatar,
  Brand,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  Dropdown,
  DropdownGroup,
  DropdownToggle,
  DropdownItem,
  Gallery,
  GalleryItem,
  KebabToggle,
  Nav,
  NavItem,
  NavList,
  Page,
  PageHeader,
  PageSection,
  PageSectionVariants,
  PageSectionTypes,
  SkipToContent,
  TextContent,
  Text,
  PageHeaderTools,
  PageHeaderToolsGroup,
  PageHeaderToolsItem
} from '@patternfly/react-core';
import { DashboardBreadcrumb } from './examples/DashboardWrapper';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import imgColorBrand from '@patternfly/react-core/src/demos/examples/pfColorLogo.svg';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';

class HorizontalNavWithSubnav extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isDropdownOpen: false,
      isKebabDropdownOpen: false,
      activeItem: 0,
      activeSubNavItem: 7
    };

    this.onDropdownToggle = isDropdownOpen => {
      this.setState({
        isDropdownOpen
      });
    };

    this.onDropdownSelect = event => {
      this.setState({
        isDropdownOpen: !this.state.isDropdownOpen
      });
    };

    this.onKebabDropdownToggle = isKebabDropdownOpen => {
      this.setState({
        isKebabDropdownOpen
      });
    };

    this.onKebabDropdownSelect = event => {
      this.setState({
        isKebabDropdownOpen: !this.state.isKebabDropdownOpen
      });
    };

    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId
      });
    };

    this.onSubNavSelect = result => {
      this.setState({
        activeSubNavItem: result.itemId
      });
    };
  }

  render() {
    const { isDropdownOpen, isKebabDropdownOpen, activeItem, activeSubNavItem } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav" variant="horizontal">
        <NavList>
          <NavItem itemId={0} isActive={activeItem === 0} to="#">
            System Panel
          </NavItem>
          <NavItem itemId={1} isActive={activeItem === 1} to="#">
            Policy
          </NavItem>
          <NavItem itemId={2} isActive={activeItem === 2} to="#">
            Authentication
          </NavItem>
          <NavItem itemId={3} isActive={activeItem === 3} to="#">
            Network Services
          </NavItem>
          <NavItem itemId={4} isActive={activeItem === 4} to="#">
            Server
          </NavItem>
          <NavItem itemId={5} isActive={activeItem === 5} to="#">
            Other horizontal nav item 1
          </NavItem>
          <NavItem itemId={6} isActive={activeItem === 6} to="#">
            Other horizontal nav item 2
          </NavItem>
        </NavList>
      </Nav>
    );
    const kebabDropdownItems = [
      <DropdownItem>
        <CogIcon /> Settings
      </DropdownItem>,
      <DropdownItem>
        <HelpIcon /> Help
      </DropdownItem>
    ];
    const userDropdownItems = [
      <DropdownGroup key="group 2">
        <DropdownItem key="group 2 profile">My profile</DropdownItem>
        <DropdownItem key="group 2 user" component="button">
          User management
        </DropdownItem>
        <DropdownItem key="group 2 logout">Logout</DropdownItem>
      </DropdownGroup>
    ];
    const headerTools = (
      <PageHeaderTools>
        <PageHeaderToolsGroup
          visibility={{
            default: 'hidden',
            lg: 'visible'
          }} /** the settings and help icon buttons are only visible on desktop sizes and replaced by a kebab dropdown for other sizes */
        >
          <PageHeaderToolsItem>
            <Button aria-label="Settings actions" variant={ButtonVariant.plain}>
              <CogIcon />
            </Button>
          </PageHeaderToolsItem>
          <PageHeaderToolsItem>
            <Button aria-label="Help actions" variant={ButtonVariant.plain}>
              <QuestionCircleIcon />
            </Button>
          </PageHeaderToolsItem>
        </PageHeaderToolsGroup>
        <PageHeaderToolsGroup>
          <PageHeaderToolsItem
            visibility={{
              lg: 'hidden'
            }} /** this kebab dropdown replaces the icon buttons and is hidden for desktop sizes */
          >
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onKebabDropdownSelect}
              toggle={<KebabToggle onToggle={this.onKebabDropdownToggle} />}
              isOpen={isKebabDropdownOpen}
              dropdownItems={kebabDropdownItems}
            />
          </PageHeaderToolsItem>
          <PageHeaderToolsItem
            visibility={{ default: 'hidden', md: 'visible' }} /** this user dropdown is hidden on mobile sizes */
          >
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onDropdownSelect}
              isOpen={isDropdownOpen}
              toggle={<DropdownToggle onToggle={this.onDropdownToggle}>John Smith</DropdownToggle>}
              dropdownItems={userDropdownItems}
            />
          </PageHeaderToolsItem>
        </PageHeaderToolsGroup>
        <Avatar src={imgAvatar} alt="Avatar image" />
      </PageHeaderTools>
    );

    const Header = (
      <PageHeader
        logo={<Brand src={imgColorBrand} alt="Patternfly Logo" />}
        headerTools={headerTools}
        topNav={PageNav}
      />
    );

    const pageId = 'main-content-page-layout-horizontal-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to content</SkipToContent>;

    const SubNav = (
      <Nav onSelect={this.onSubNavSelect} aria-label="Nav" variant="horizontal-subnav">
        <NavList>
          <NavItem itemId={7} isActive={activeSubNavItem === 7} to="#">
            Horizontal subnav item 1
          </NavItem>
          <NavItem itemId={8} isActive={activeSubNavItem === 8} to="#">
            Horizontal subnav item 2
          </NavItem>
          <NavItem itemId={9} isActive={activeSubNavItem === 9} to="#">
            Horizontal subnav item 3
          </NavItem>
          <NavItem itemId={10} isActive={activeSubNavItem === 10} to="#">
            Horizontal subnav item 4
          </NavItem>
          <NavItem itemId={11} isActive={activeSubNavItem === 11} to="#">
            Horizontal subnav item 5
          </NavItem>
          <NavItem itemId={12} isActive={activeSubNavItem === 12} to="#">
            Horizontal subnav item 6
          </NavItem>
          <NavItem itemId={13} isActive={activeSubNavItem === 13} to="#">
            Horizontal subnav item 7
          </NavItem>
        </NavList>
      </Nav>
    );

    return (
      <React.Fragment>
        <Page header={Header} skipToContent={PageSkipToContent} mainContainerId={pageId}>
          <PageSection type={PageSectionTypes.subNav} isWidthLimited>
            {SubNav}
          </PageSection>
          <PageSection type={PageSectionTypes.breadcrumb} isWidthLimited>
            {DashboardBreadcrumb}
          </PageSection>
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of it’s relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(10)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Legacy tertiary nav

```js isFullscreen
import React from 'react';
import {
  Card,
  CardBody,
  Gallery,
  GalleryItem,
  Nav,
  NavItem,
  NavList,
  Page,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  SkipToContent,
  TextContent,
  Text
} from '@patternfly/react-core';
import { DashboardBreadcrumb } from './examples/DashboardWrapper';
import DashboardHeader from './examples/DashboardHeader';

class PageLayoutTertiaryNav extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      activeItem: 0
    };

    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId
      });
    };
  }

  render() {
    const { activeItem } = this.state;

    const PageNav = (
      <Nav variant="tertiary" onSelect={this.onNavSelect} aria-label="Nav">
        <NavList>
          <NavItem itemId={0} isActive={activeItem === 0} to="#">
            System Panel
          </NavItem>
          <NavItem itemId={1} isActive={activeItem === 1} to="#">
            Policy
          </NavItem>
          <NavItem itemId={2} isActive={activeItem === 2} to="#">
            Authentication
          </NavItem>
          <NavItem itemId={3} isActive={activeItem === 3} to="#">
            Network Services
          </NavItem>
          <NavItem itemId={4} isActive={activeItem === 4} to="#">
            Server
          </NavItem>
        </NavList>
      </Nav>
    );

    const pageId = 'main-content-page-layout-tertiary-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to content</SkipToContent>;

    return (
      <React.Fragment>
        <Page
          header={<DashboardHeader />}
          breadcrumb={DashboardBreadcrumb}
          tertiaryNav={PageNav}
          isManagedSidebar
          isTertiaryNavWidthLimited
          skipToContent={PageSkipToContent}
          mainContainerId={pageId}
        >
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of its relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(10)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Legacy/Light Nav

```js isFullscreen
import React from 'react';
import {
  Card,
  CardBody,
  Gallery,
  GalleryItem,
  Nav,
  NavItem,
  NavList,
  Page,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  SkipToContent,
  TextContent,
  Text
} from '@patternfly/react-core';
import DashboardHeader from './examples/DashboardHeader';

class PageLayoutLightNav extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      activeItem: 0
    };

    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId
      });
    };
  }

  render() {
    const { activeItem } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav" theme="light">
        <NavList>
          <NavItem itemId={0} isActive={activeItem === 0} to="#">
            System Panel
          </NavItem>
          <NavItem itemId={1} isActive={activeItem === 1} to="#">
            Policy
          </NavItem>
          <NavItem itemId={2} isActive={activeItem === 2} to="#">
            Authentication
          </NavItem>
          <NavItem itemId={3} isActive={activeItem === 3} to="#">
            Network Services
          </NavItem>
          <NavItem itemId={4} isActive={activeItem === 4} to="#">
            Server
          </NavItem>
        </NavList>
      </Nav>
    );

    const Sidebar = <PageSidebar nav={PageNav} theme="light" />;
    const pageId = 'main-content-page-layout-simple-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to Content</SkipToContent>;

    return (
      <React.Fragment>
        <Page
          header={<DashboardHeader />}
          sidebar={Sidebar}
          isManagedSidebar
          skipToContent={PageSkipToContent}
          mainContainerId={pageId}
        >
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of its relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(10)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Manual nav

```js isFullscreen
import React from 'react';
import {
  Avatar,
  Brand,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  Dropdown,
  DropdownGroup,
  DropdownToggle,
  DropdownItem,
  Gallery,
  GalleryItem,
  KebabToggle,
  Nav,
  NavItem,
  NavList,
  Page,
  PageHeader,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  SkipToContent,
  TextContent,
  Text,
  PageHeaderTools,
  PageHeaderToolsGroup,
  PageHeaderToolsItem
} from '@patternfly/react-core';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import imgBrand from '@patternfly/react-core/src/components/Brand/examples/pfLogo.svg';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';

class PageLayoutManualNav extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isDropdownOpen: false,
      isKebabDropdownOpen: false,
      activeItem: 0,
      isMobileView: false,
      isNavOpenDesktop: true,
      isNavOpenMobile: false
    };

    this.onDropdownToggle = isDropdownOpen => {
      this.setState({
        isDropdownOpen
      });
    };

    this.onDropdownSelect = event => {
      this.setState({
        isDropdownOpen: !this.state.isDropdownOpen
      });
    };

    this.onKebabDropdownToggle = isKebabDropdownOpen => {
      this.setState({
        isKebabDropdownOpen
      });
    };

    this.onKebabDropdownSelect = event => {
      this.setState({
        isKebabDropdownOpen: !this.state.isKebabDropdownOpen
      });
    };

    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId
      });
    };

    this.onNavToggleDesktop = () => {
      this.setState({
        isNavOpenDesktop: !this.state.isNavOpenDesktop
      });
    };

    this.onNavToggleMobile = () => {
      this.setState({
        isNavOpenMobile: !this.state.isNavOpenMobile
      });
    };

    this.onPageResize = ({ mobileView, windowSize }) => {
      this.setState({
        isMobileView: mobileView
      });
    };
  }

  render() {
    const {
      isDropdownOpen,
      isKebabDropdownOpen,
      activeItem,
      isNavOpenDesktop,
      isNavOpenMobile,
      isMobileView
    } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavList>
          <NavItem itemId={0} isActive={activeItem === 0} to="#">
            System Panel
          </NavItem>
          <NavItem itemId={1} isActive={activeItem === 1} to="#">
            Policy
          </NavItem>
          <NavItem itemId={2} isActive={activeItem === 2} to="#">
            Authentication
          </NavItem>
          <NavItem itemId={3} isActive={activeItem === 3} to="#">
            Network Services
          </NavItem>
          <NavItem itemId={4} isActive={activeItem === 4} to="#">
            Server
          </NavItem>
        </NavList>
      </Nav>
    );
    const kebabDropdownItems = [
      <DropdownItem>
        <CogIcon /> Settings
      </DropdownItem>,
      <DropdownItem>
        <HelpIcon /> Help
      </DropdownItem>
    ];
    const userDropdownItems = [
      <DropdownGroup key="group 2">
        <DropdownItem key="group 2 profile">My profile</DropdownItem>
        <DropdownItem key="group 2 user" component="button">
          User management
        </DropdownItem>
        <DropdownItem key="group 2 logout">Logout</DropdownItem>
      </DropdownGroup>
    ];
    const headerTools = (
      <PageHeaderTools>
        <PageHeaderToolsGroup
          visibility={{
            default: 'hidden',
            lg: 'visible'
          }} /** the settings and help icon buttons are only visible on desktop sizes and replaced by a kebab dropdown for other sizes */
        >
          <PageHeaderToolsItem>
            <Button aria-label="Settings actions" variant={ButtonVariant.plain}>
              <CogIcon />
            </Button>
          </PageHeaderToolsItem>
          <PageHeaderToolsItem>
            <Button aria-label="Help actions" variant={ButtonVariant.plain}>
              <QuestionCircleIcon />
            </Button>
          </PageHeaderToolsItem>
        </PageHeaderToolsGroup>
        <PageHeaderToolsGroup>
          <PageHeaderToolsItem
            visibility={{
              default: 'hidden',
              breakpoint: 'lg'
            }} /** this kebab dropdown replaces the icon buttons and is hidden for desktop sizes */
          >
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onKebabDropdownSelect}
              toggle={<KebabToggle onToggle={this.onKebabDropdownToggle} />}
              isOpen={isKebabDropdownOpen}
              dropdownItems={kebabDropdownItems}
            />
          </PageHeaderToolsItem>
          <PageHeaderToolsItem
            visibility={{ default: 'hidden', md: 'visible' }} /** this user dropdown is hidden on mobile sizes */
          >
            <Dropdown
              isPlain
              position="right"
              onSelect={this.onDropdownSelect}
              isOpen={isDropdownOpen}
              toggle={<DropdownToggle onToggle={this.onDropdownToggle}>John Smith</DropdownToggle>}
              dropdownItems={userDropdownItems}
            />
          </PageHeaderToolsItem>
        </PageHeaderToolsGroup>
        <Avatar src={imgAvatar} alt="Avatar image" />
      </PageHeaderTools>
    );

    const Header = (
      <PageHeader
        logo={<Brand src={imgBrand} alt="Patternfly Logo" />}
        headerTools={headerTools}
        showNavToggle
        onNavToggle={isMobileView ? this.onNavToggleMobile : this.onNavToggleDesktop}
        isNavOpen={isMobileView ? isNavOpenMobile : isNavOpenDesktop}
      />
    );
    const Sidebar = <PageSidebar nav={PageNav} isNavOpen={isMobileView ? isNavOpenMobile : isNavOpenDesktop} />;
    const pageId = 'main-content-page-layout-manual-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to Content</SkipToContent>;

    return (
      <React.Fragment>
        <Page
          header={Header}
          sidebar={Sidebar}
          onPageResize={this.onPageResize}
          skipToContent={PageSkipToContent}
          mainContainerId={pageId}
        >
          <PageSection variant={PageSectionVariants.light}>
            <TextContent>
              <Text component="h1">Main title</Text>
              <Text component="p">
                Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                of its relative line height of 1.5.
              </Text>
            </TextContent>
          </PageSection>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(10)).map((x, i) => (
                <GalleryItem key={i}>
                  <Card>
                    <CardBody>This is a card</CardBody>
                  </Card>
                </GalleryItem>
              ))}
            </Gallery>
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Flyout nav

```js isFullscreen
import React from 'react';
import {
  Page,
  Nav,
  NavList,
  NavItem,
  PageHeader,
  PageHeaderTools,
  PageSidebar,
  PageSection,
  PageSectionVariants,
  Menu,
  MenuContent,
  MenuList,
  MenuItem
} from '@patternfly/react-core';
import DashboardHeader from './examples/DashboardHeader';

class VerticalPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isNavOpen: true,
      activeItem: 0
    };
    this.onNavSelect = result => {
      this.setState({
        activeItem: result.itemId
      });
    };
    this.onMenuSelect = (event, itemId) => {
      this.setState({
        activeItem: itemId
      });
    };
  }

  render() {
    const numFlyouts = 5;
    const FlyoutMenu = ({ depth, children }) => (
      <Menu key={depth} containsFlyout isNavFlyout id={`menu-${depth}`} onSelect={this.onMenuSelect}>
        <MenuContent>
          <MenuList>
            <MenuItem flyoutMenu={children} itemId={`next-menu-${depth}`} to={`#menu-link-${depth}`}>
              Additional settings
            </MenuItem>
            {[...Array(numFlyouts - depth).keys()].map(j => (
              <MenuItem key={`${depth}-${j}`} itemId={`${depth}-${j}`} to={`#menu-link-${depth}-${j}`}>
                Settings menu {depth} item {j}
              </MenuItem>
            ))}
            <MenuItem flyoutMenu={children} itemId={`next-menu-2-${depth}`} to={`#second-menu-link-${depth}`}>
              Additional settings
            </MenuItem>
          </MenuList>
        </MenuContent>
      </Menu>
    );
    let curFlyout = <FlyoutMenu depth={1} />;
    for (let i = 2; i < numFlyouts - 1; i++) {
      curFlyout = <FlyoutMenu depth={i}>{curFlyout}</FlyoutMenu>;
    }

    const { activeItem } = this.state;

    const Sidebar = (
      <PageSidebar
        nav={
          <Nav onSelect={this.onNavSelect}>
            <NavList>
              <NavItem id="flyout-link1" to="#flyout-link1" itemId={0} isActive={activeItem === 0}>
                System Panel
              </NavItem>
              <NavItem flyout={curFlyout} id="flyout-link2" to="#flyout-link2" itemId={1} isActive={activeItem === 1}>
                Settings
              </NavItem>
              <NavItem id="flyout-link3" to="#flyout-link3" itemId={2} isActive={activeItem === 2}>
                Authentication
              </NavItem>
            </NavList>
          </Nav>
        }
      />
    );

    return (
      <Page header={<DashboardHeader />} sidebar={Sidebar} isManagedSidebar>
        <PageSection variant={PageSectionVariants.darker}>Section with darker background</PageSection>
        <PageSection variant={PageSectionVariants.dark}>Section with dark background</PageSection>
        <PageSection variant={PageSectionVariants.light}>Section with light background</PageSection>
      </Page>
    );
  }
}
```

### Drilldown nav

```js isBeta isFullscreen file="./examples/Nav/NavDrilldown.tsx"
```
