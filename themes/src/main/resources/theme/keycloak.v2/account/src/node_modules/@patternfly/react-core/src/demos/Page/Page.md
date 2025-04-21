---
id: Page
section: components
---

import { css } from '@patternfly/react-styles';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import imgBrand from '@patternfly/react-core/src/demos/examples/pfColorLogo.svg';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';
import AttentionBellIcon from '@patternfly/react-icons/dist/esm/icons/attention-bell-icon';
import LightbulbIcon from '@patternfly/react-icons/dist/esm/icons/lightbulb-icon';

- All but the last example set the `isManagedSidebar` prop on the Page component to have the sidebar automatically close for smaller screen widths. You can also manually control this behavior by not adding the `isManagedSidebar` prop and instead:

  1. Add an onNavToggle callback to PageHeader
  1. Pass a boolean into the isNavOpen prop to PageSidebar

  The last example demonstrates this.

- To make the page take up the full height, it is recommended to set the height of all ancestor elements up to the page component to `100%`

## Layouts

This demonstrates a variety of navigation patterns in the context of a full page layout. These can be used as a basis for choosing the most appropriate page template for your application.

### Sticky section group

```js isFullscreen
import React from 'react';
import {
  Avatar,
  Brand,
  Breadcrumb,
  BreadcrumbItem,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  Checkbox,
  Divider,
  Dropdown,
  DropdownGroup,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  Gallery,
  GalleryItem,
  KebabToggle,
  Masthead,
  MastheadBrand,
  MastheadContent,
  MastheadMain,
  MastheadToggle,
  Nav,
  NavItem,
  NavList,
  Page,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  PageToggleButton,
  SkipToContent,
  TextContent,
  Text,
  Toolbar,
  ToolbarContent,
  ToolbarGroup,
  ToolbarItem,
  Drawer,
  DrawerPanelContent,
  DrawerContent,
  DrawerContentBody,
  DrawerHead,
  DrawerActions,
  DrawerCloseButton,
} from '@patternfly/react-core';
import { css } from '@patternfly/react-styles';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import AttentionBellIcon from '@patternfly/react-icons/dist/esm/icons/attention-bell-icon';
import LightbulbIcon from '@patternfly/react-icons/dist/esm/icons/lightbulb-icon';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';
import imgBrand from './imgBrand.svg';
import imgAvatar from './imgAvatar.svg';

class PageLayoutGrouped extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isDropdownOpen: false,
      isKebabDropdownOpen: false,
      isFullKebabDropdownOpen: false,
      activeItem: 0,
      isDrawerExpanded: false
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

    this.onFullKebabToggle = isFullKebabDropdownOpen => {
      this.setState({
        isFullKebabDropdownOpen
      });
    };

    this.onFullKebabSelect = () => {
      this.setState({
        isFullKebabDropdownOpen: !this.state.isFullKebabDropdownOpen
      });
    };

    this.onDrawerToggle = () => {
      const isDrawerExpanded = !this.state.isDrawerExpanded;
      this.setState({
        isDrawerExpanded
      });
    };

    this.onDrawerClose = () => {
      this.setState({
        isDrawerExpanded: false
      });
    };
  }

  render() {
    const { isDropdownOpen, isKebabDropdownOpen, activeItem, isFullKebabDropdownOpen, isDrawerExpanded } = this.state;

    const PageNav = (
      <Nav variant="tertiary" onSelect={this.onNavSelect} aria-label="Nav">
        <NavList>
          <NavItem href="#" itemId={0} isActive={activeItem === 0}>
            System panel
          </NavItem>
          <NavItem href="#" itemId={1} isActive={activeItem === 1}>
            Policy
          </NavItem>
          <NavItem href="#" itemId={2} isActive={activeItem === 2}>
            Authentication
          </NavItem>
          <NavItem href="#" itemId={3} isActive={activeItem === 3}>
            Network services
          </NavItem>
          <NavItem href="#" itemId={4} isActive={activeItem === 4}>
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

    const fullKebabItems = [
      <DropdownGroup key="group 2">
        <DropdownItem key="group 2 profile">My profile</DropdownItem>
        <DropdownItem key="group 2 user" component="button">
          User management
        </DropdownItem>
        <DropdownItem key="group 2 logout">Logout</DropdownItem>
      </DropdownGroup>,
      <Divider key="divider" />,
      <DropdownItem key="kebab-1">
        <CogIcon /> Settings
      </DropdownItem>,
      <DropdownItem key="kebab-2">
        <HelpIcon /> Help
      </DropdownItem>
    ];

    const headerToolbar = (
      <Toolbar id="toolbar" isFullHeight isStatic>
        <ToolbarContent>
          <ToolbarGroup
            variant="icon-button-group"
            alignment={{ default: 'alignRight' }}
            spacer={{ default: 'spacerNone', md: 'spacerMd' }}
          >
            <ToolbarItem>
              <Button aria-label="Toggle drawer" variant={ButtonVariant.plain} onClick={this.onDrawerToggle}>
                <LightbulbIcon color={isDrawerExpanded ? 'yellow' : 'currentColor'} />
              </Button>
            </ToolbarItem>
            <ToolbarItem>
              <Button aria-label="Notifications" variant={ButtonVariant.plain}>
                <AttentionBellIcon />
              </Button>
            </ToolbarItem>
            <ToolbarGroup variant="icon-button-group" visibility={{ default: 'hidden', lg: 'visible' }}>
              <ToolbarItem>
                <Button aria-label="Settings actions" variant={ButtonVariant.plain}>
                  <CogIcon />
                </Button>
              </ToolbarItem>
              <ToolbarItem>
                <Button aria-label="Help actions" variant={ButtonVariant.plain}>
                  <QuestionCircleIcon />
                </Button>
              </ToolbarItem>
            </ToolbarGroup>
            <ToolbarItem visibility={{ default: 'hidden', md: 'visible', lg: 'hidden' }}>
              <Dropdown
                isPlain
                position="right"
                onSelect={this.onKebabDropdownSelect}
                toggle={<KebabToggle onToggle={this.onKebabDropdownToggle} />}
                isOpen={isKebabDropdownOpen}
                dropdownItems={kebabDropdownItems}
              />
            </ToolbarItem>
            <ToolbarItem visibility={{ default: 'visible', md: 'hidden', lg: 'hidden', xl: 'hidden', '2xl': 'hidden' }}>
              <Dropdown
                isPlain
                position="right"
                onSelect={this.onFullKebabSelect}
                toggle={<KebabToggle onToggle={this.onFullKebabToggle} />}
                isOpen={isFullKebabDropdownOpen}
                dropdownItems={fullKebabItems}
              />
            </ToolbarItem>
          </ToolbarGroup>
          <ToolbarItem visibility={{ default: 'hidden', md: 'visible' }}>
            <Dropdown
              position="right"
              onSelect={this.onDropdownSelect}
              isOpen={isDropdownOpen}
              toggle={
                <DropdownToggle icon={<Avatar src={imgAvatar} alt="Avatar" />} onToggle={this.onDropdownToggle}>
                  John Smith
                </DropdownToggle>
              }
              dropdownItems={userDropdownItems}
            />
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    );

    const Header = (
      <Masthead>
        <MastheadToggle>
          <PageToggleButton variant="plain" aria-label="Global navigation">
            <BarsIcon />
          </PageToggleButton>
        </MastheadToggle>
        <MastheadMain>
          <MastheadBrand>
            <Brand src={imgBrand} alt="Patternfly Logo" />
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>{headerToolbar}</MastheadContent>
      </Masthead>
    );

    const pageId = 'main-content-page-layout-tertiary-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to content</SkipToContent>;

    const PageBreadcrumb = (
      <Breadcrumb>
        <BreadcrumbItem>Section home</BreadcrumbItem>
        <BreadcrumbItem to="#">Section title</BreadcrumbItem>
        <BreadcrumbItem to="#">Section title</BreadcrumbItem>
        <BreadcrumbItem to="#" isActive>
          Section landing
        </BreadcrumbItem>
      </Breadcrumb>
    );

    const panelContent = (
      <DrawerPanelContent isResizable>
        <DrawerHead>
          <span tabIndex={isDrawerExpanded ? 0 : -1}>
            drawer-panel
          </span>
          <DrawerActions>
            <DrawerCloseButton onClick={this.onDrawerClose} />
          </DrawerActions>
        </DrawerHead>
      </DrawerPanelContent>
    );

    const Sidebar = <PageSidebar nav="Navigation" />;

    return (
      <Drawer isExpanded={isDrawerExpanded} isInline onExpand={this.onExpand}>
        <DrawerContent panelContent={panelContent}>
          <DrawerContentBody>
            <Page
              header={Header}
              breadcrumb={PageBreadcrumb}
              sidebar={Sidebar}
              tertiaryNav={PageNav}
              isManagedSidebar
              isTertiaryNavWidthLimited
              isBreadcrumbWidthLimited
              skipToContent={PageSkipToContent}
              mainContainerId={pageId}
              isTertiaryNavGrouped
              isBreadcrumbGrouped
              additionalGroupedContent={
                <PageSection variant={PageSectionVariants.light}>
                  <TextContent>
                    <Text component="h1">Main title</Text>
                    <Text component="p">
                      Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                      of its relative line height of 1.5.
                    </Text>
                  </TextContent>
                </PageSection>
              }
              groupProps={{
                sticky: 'top'
              }}
            >
              <PageSection>
                <Gallery hasGutter>
                  {Array.apply(0, Array(20)).map((x, i) => (
                    <GalleryItem key={i}>
                      <Card>
                        <CardBody>This is a card</CardBody>
                      </Card>
                    </GalleryItem>
                  ))}
                </Gallery>
              </PageSection>
            </Page>
          </DrawerContentBody>
        </DrawerContent>
      </Drawer>
    );
  }
}
```

### Sticky section group (using PageHeader)

This demo is provided becuase PageHeader and PageHeaderTools are still in use; however, going forward Masthead and Toolbar should be used to make headers rather than PageHeader and PageHeaderTools.

```js isFullscreen
import React from 'react';
import {
  Avatar,
  Brand,
  Breadcrumb,
  BreadcrumbItem,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  Dropdown,
  DropdownGroup,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
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
import { css } from '@patternfly/react-styles';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import imgBrand from './imgBrand.svg';
import imgAvatar from './imgAvatar.svg';

class PageLayoutGrouped extends React.Component {
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
      <Nav variant="tertiary" onSelect={this.onNavSelect} aria-label="Nav">
        <NavList>
          <NavItem href="#" itemId={0} isActive={activeItem === 0}>
            System panel
          </NavItem>
          <NavItem href="#" itemId={1} isActive={activeItem === 1}>
            Policy
          </NavItem>
          <NavItem href="#" itemId={2} isActive={activeItem === 2}>
            Authentication
          </NavItem>
          <NavItem href="#" itemId={3} isActive={activeItem === 3}>
            Network services
          </NavItem>
          <NavItem href="#" itemId={4} isActive={activeItem === 4}>
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
      <PageHeader logo={<Brand src={imgBrand} alt="Patternfly Logo" />} headerTools={headerTools} showNavToggle />
    );
    const pageId = 'main-content-page-layout-tertiary-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to content</SkipToContent>;

    const PageBreadcrumb = (
      <Breadcrumb>
        <BreadcrumbItem>Section home</BreadcrumbItem>
        <BreadcrumbItem to="#">Section title</BreadcrumbItem>
        <BreadcrumbItem to="#">Section title</BreadcrumbItem>
        <BreadcrumbItem to="#" isActive>
          Section landing
        </BreadcrumbItem>
      </Breadcrumb>
    );

    return (
      <React.Fragment>
        <Page
          header={Header}
          breadcrumb={PageBreadcrumb}
          tertiaryNav={PageNav}
          isManagedSidebar
          isTertiaryNavWidthLimited
          isBreadcrumbWidthLimited
          skipToContent={PageSkipToContent}
          mainContainerId={pageId}
          isTertiaryNavGrouped
          isBreadcrumbGrouped
          additionalGroupedContent={
            <PageSection variant={PageSectionVariants.light}>
              <TextContent>
                <Text component="h1">Main title</Text>
                <Text component="p">
                  Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                  of its relative line height of 1.5.
                </Text>
              </TextContent>
            </PageSection>
          }
          groupProps={{
            sticky: 'top'
          }}
        >
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(20)).map((x, i) => (
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

### Sticky section group (alternate syntax and using PageHeader)

Please see <a href="#sticky-section-group-using-pageheader">this</a> note regarding PageHeader.

```js isFullscreen
import React from 'react';
import {
  Avatar,
  Brand,
  Breadcrumb,
  BreadcrumbItem,
  Button,
  ButtonVariant,
  Card,
  CardBody,
  Dropdown,
  DropdownGroup,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
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
  PageGroup,
  PageBreadcrumb,
  PageNavigation,
  SkipToContent,
  TextContent,
  Text,
  PageHeaderTools,
  PageHeaderToolsGroup,
  PageHeaderToolsItem
} from '@patternfly/react-core';
import { css } from '@patternfly/react-styles';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import imgBrand from './imgBrand.svg';
import imgAvatar from './imgAvatar.svg';

class PageLayoutGroupedAlt extends React.Component {
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
      <PageHeader logo={<Brand src={imgBrand} alt="Patternfly Logo" />} headerTools={headerTools} showNavToggle />
    );
    const pageId = 'main-content-page-layout-tertiary-nav';
    const PageSkipToContent = <SkipToContent href={`#${pageId}`}>Skip to content</SkipToContent>;

    return (
      <React.Fragment>
        <Page header={Header} isManagedSidebar skipToContent={PageSkipToContent} mainContainerId={pageId}>
          <PageGroup sticky="top">
            <PageNavigation isWidthLimited>
              <Nav variant="tertiary" onSelect={this.onNavSelect} aria-label="Nav">
                <NavList>
                  <NavItem href="#" itemId={0} isActive={activeItem === 0}>
                    System panel
                  </NavItem>
                  <NavItem href="#" itemId={1} isActive={activeItem === 1}>
                    Policy
                  </NavItem>
                  <NavItem href="#" itemId={2} isActive={activeItem === 2}>
                    Authentication
                  </NavItem>
                  <NavItem href="#" itemId={3} isActive={activeItem === 3}>
                    Network services
                  </NavItem>
                  <NavItem href="#" itemId={4} isActive={activeItem === 4}>
                    Server
                  </NavItem>
                </NavList>
              </Nav>
            </PageNavigation>
            <PageBreadcrumb>
              <Breadcrumb>
                <BreadcrumbItem>Section home</BreadcrumbItem>
                <BreadcrumbItem to="#">Section title</BreadcrumbItem>
                <BreadcrumbItem to="#">Section title</BreadcrumbItem>
                <BreadcrumbItem to="#" isActive>
                  Section landing
                </BreadcrumbItem>
              </Breadcrumb>
            </PageBreadcrumb>
            <PageSection variant={PageSectionVariants.light}>
              <TextContent>
                <Text component="h1">Main title</Text>
                <Text component="p">
                  Body text should be Overpass Regular at 16px. It should have leading of 24px because <br />
                  of its relative line height of 1.5.
                </Text>
              </TextContent>
            </PageSection>{' '}
          </PageGroup>
          <PageSection>
            <Gallery hasGutter>
              {Array.apply(0, Array(20)).map((x, i) => (
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
