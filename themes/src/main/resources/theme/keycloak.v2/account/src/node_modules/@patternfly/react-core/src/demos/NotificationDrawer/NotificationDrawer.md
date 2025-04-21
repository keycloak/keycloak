---
id: Notification drawer
section: components
beta: true
---

import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import imgBrand from '@patternfly/react-core/src/components/Brand/examples/pfLogo.svg';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';
import AttentionBellIcon from '@patternfly/react-icons/dist/esm/icons/attention-bell-icon';

## Demos

- Focus must be manually managed when the NotificationDrawer component is opened:

  1. Create a React `ref` and pass it into the NotificationDrawer component's `ref` attribute
  2. Pass in a function to the `onNotificationDrawerExpand` prop of the Page component that will place focus on the first interact-able element inside the NotificationDrawer component via the previously created `ref`

### Basic

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
  Drawer,
  DrawerContent,
  DrawerContentBody,
  Dropdown,
  DropdownGroup,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStatePrimary,
  Gallery,
  GalleryItem,
  KebabToggle,
  Nav,
  NavItem,
  NavList,
  NotificationBadge,
  NotificationDrawer,
  NotificationDrawerBody,
  NotificationDrawerHeader,
  NotificationDrawerList,
  NotificationDrawerListItem,
  NotificationDrawerListItemBody,
  NotificationDrawerListItemHeader,
  Page,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  SkipToContent,
  TextContent,
  Text,
  Title,
  PageToggleButton,
  Masthead,
  MastheadMain,
  MastheadToggle,
  MastheadContent,
  MastheadBrand,
  Toolbar,
  ToolbarItem,
  ToolbarGroup,
  ToolbarContent
} from '@patternfly/react-core';
import { css } from '@patternfly/react-styles';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';
import imgBrand from '@patternfly/react-core/src/components/Brand/examples/pfLogo.svg';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';
import { Table, TableHeader, TableBody } from '@patternfly/react-table';

class BasicNotificationDrawer extends React.Component {
  constructor(props) {
    super(props);
    this.drawerRef = React.createRef();
    this.state = {
      isDropdownOpen: false,
      isKebabDropdownOpen: false,
      activeItem: 0,
      isDrawerExpanded: false,
      isUnreadMap: {
        'notification-1': true,
        'notification-2': true
      },
      showNotifications: true,
      isActionsMenuOpen: null
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

    this.onCloseNotificationDrawer = () => {
      this.setState(prevState => {
        return {
          isDrawerExpanded: !prevState.isDrawerExpanded
        };
      });
    };

    this.onToggle = (id, isOpen) => {
      this.setState({
        isActionsMenuOpen: { [id]: isOpen }
      });
    };

    this.onSelect = event => {
      this.setState({
        isActionsMenuOpen: null
      });
    };

    this.onListItemClick = id => {
      this.setState(prevState => {
        if (!prevState.isUnreadMap) return;
        prevState.isUnreadMap[id] = false;
        return {
          isUnreadMap: prevState.isUnreadMap
        };
      });
    };

    this.getNumberUnread = () => {
      const { isUnreadMap } = this.state;
      if (isUnreadMap === null) return 0;
      return Object.keys(isUnreadMap).reduce((count, id) => {
        return isUnreadMap[id] ? count + 1 : count;
      }, 0);
    };

    this.markAllRead = () => {
      this.setState({
        isUnreadMap: null
      });
    };

    this.showNotifications = showNotifications => {
      this.setState({
        isUnreadMap: null,
        showNotifications: showNotifications
      });
    };

    this.focusDrawer = () => {
      const firstTabbableItem = this.drawerRef.current.querySelector('a, button');
      firstTabbableItem.focus();
    };
  }

  render() {
    const {
      isDropdownOpen,
      isKebabDropdownOpen,
      activeItem,
      res,
      isDrawerExpanded,
      isActionsMenuOpen,
      isUnreadMap,
      showNotifications
    } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavList>
          <NavItem itemId={0} isActive={activeItem === 0}>
            System Panel
          </NavItem>
          <NavItem itemId={1} isActive={activeItem === 1}>
            Policy
          </NavItem>
          <NavItem itemId={2} isActive={activeItem === 2}>
            Authentication
          </NavItem>
          <NavItem itemId={3} isActive={activeItem === 3}>
            Network Services
          </NavItem>
          <NavItem itemId={4} isActive={activeItem === 4}>
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
    const headerToolbar = (
      <Toolbar>
        <ToolbarContent>
          <ToolbarGroup spaceItems={{ default: 'spacerNone' }} alignment={{ default: 'alignRight' }}>
            <ToolbarGroup variant="icon-button-group">
              <ToolbarItem visibility={{ default: 'visible' }} isSelected={isDrawerExpanded}>
                <NotificationBadge
                  variant={this.getNumberUnread() === 0 ? 'read' : 'unread'}
                  onClick={this.onCloseNotificationDrawer}
                  aria-label="Notifications"
                >
                  <BellIcon />
                </NotificationBadge>
              </ToolbarItem>
              <ToolbarGroup
                variant="icon-button-group"
                visibility={{
                  default: 'hidden',
                  lg: 'visible'
                }} /** the settings and help icon buttons are only visible on desktop sizes and replaced by a kebab dropdown for other sizes */
              >
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
            </ToolbarGroup>
            <ToolbarGroup>
              <ToolbarItem
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
              </ToolbarItem>
              <ToolbarItem
                visibility={{ default: 'hidden', md: 'visible' }} /** this user dropdown is hidden on mobile sizes */
              >
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
            </ToolbarGroup>
          </ToolbarGroup>
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
            <Brand src={imgBrand} alt="Patternfly logo" />
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>{headerToolbar}</MastheadContent>
      </Masthead>
    );
    const Sidebar = <PageSidebar nav={PageNav} />;
    const pageId = 'main-content-page-layout-default-nav';
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

    const drawerContent = 'Panel content';

    const notificationDrawerActions = [
      <DropdownItem key="markAllRead" onClick={this.markAllRead} component="button">
        Mark all read
      </DropdownItem>,
      <DropdownItem key="clearAll" onClick={() => this.showNotifications(false)} component="button">
        Clear all
      </DropdownItem>,
      <DropdownItem key="unclearLast" onClick={() => this.showNotifications(true)} component="button">
        Unclear last
      </DropdownItem>,
      <DropdownItem key="settings" component="button">
        Settings
      </DropdownItem>
    ];

    const notificationDrawerDropdownItems = [
      <DropdownItem key="link">Link</DropdownItem>,
      <DropdownItem key="action" component="button">
        Action
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem key="disabled link" isDisabled>
        Disabled Link
      </DropdownItem>
    ];

    const notificationDrawer = (
      <NotificationDrawer ref={this.drawerRef}>
        <NotificationDrawerHeader count={this.getNumberUnread()} onClose={this.onCloseNotificationDrawer}>
          <Dropdown
            onSelect={this.onSelect}
            toggle={<KebabToggle onToggle={isOpen => this.onToggle('toggle-id-0', isOpen)} id="toggle-id-0" />}
            isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-0']}
            isPlain
            dropdownItems={notificationDrawerActions}
            id="notification-0"
            position={DropdownPosition.right}
          />
        </NotificationDrawerHeader>
        <NotificationDrawerBody>
          {showNotifications && (
            <NotificationDrawerList>
              <NotificationDrawerListItem
                variant="info"
                onClick={() => this.onListItemClick('notification-1')}
                isRead={isUnreadMap === null || !isUnreadMap['notification-1']}
              >
                <NotificationDrawerListItemHeader
                  variant="info"
                  title="Unread info notification title"
                  srTitle="Info notification:"
                >
                  <Dropdown
                    position={DropdownPosition.right}
                    onSelect={this.onSelect}
                    toggle={<KebabToggle onToggle={isOpen => this.onToggle('toggle-id-1', isOpen)} id="toggle-id-1" />}
                    isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-1']}
                    isPlain
                    dropdownItems={notificationDrawerDropdownItems}
                    id="notification-1"
                  />
                </NotificationDrawerListItemHeader>
                <NotificationDrawerListItemBody timestamp="5 minutes ago">
                  This is an info notification description.
                </NotificationDrawerListItemBody>
              </NotificationDrawerListItem>
              <NotificationDrawerListItem
                variant="danger"
                onClick={() => this.onListItemClick('notification-2')}
                isRead={isUnreadMap === null || !isUnreadMap['notification-2']}
              >
                <NotificationDrawerListItemHeader
                  variant="danger"
                  title="Unread danger notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines."
                  srTitle="Danger notification:"
                >
                  <Dropdown
                    position={DropdownPosition.right}
                    onSelect={this.onSelect}
                    toggle={<KebabToggle onToggle={isOpen => this.onToggle('toggle-id-2', isOpen)} id="toggle-id-2" />}
                    isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-2']}
                    isPlain
                    dropdownItems={notificationDrawerDropdownItems}
                    id="notification-2"
                  />
                </NotificationDrawerListItemHeader>
                <NotificationDrawerListItemBody timestamp="10 minutes ago">
                  This is a danger notification description. This is a long description to show how the title will wrap
                  if it is long and wraps to multiple lines.
                </NotificationDrawerListItemBody>
              </NotificationDrawerListItem>
              <NotificationDrawerListItem
                variant="warning"
                onClick={() => this.onListItemClick('notification-3')}
                isRead={isUnreadMap === null || !isUnreadMap['notification-3']}
              >
                <NotificationDrawerListItemHeader
                  variant="warning"
                  title="Read warning notification title"
                  srTitle="Warning notification:"
                >
                  <Dropdown
                    position={DropdownPosition.right}
                    onSelect={this.onSelect}
                    toggle={<KebabToggle onToggle={isOpen => this.onToggle('toggle-id-3', isOpen)} id="toggle-id-3" />}
                    isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-3']}
                    isPlain
                    dropdownItems={notificationDrawerDropdownItems}
                    id="notification-3"
                  />
                </NotificationDrawerListItemHeader>
                <NotificationDrawerListItemBody timestamp="20 minutes ago">
                  This is a warning notification description.
                </NotificationDrawerListItemBody>
              </NotificationDrawerListItem>
              <NotificationDrawerListItem
                variant="success"
                onClick={() => this.onListItemClick('notification-4')}
                isRead={isUnreadMap === null || !isUnreadMap['notification-4']}
              >
                <NotificationDrawerListItemHeader
                  variant="success"
                  title="Read success notification title"
                  srTitle="Success notification:"
                >
                  <Dropdown
                    position={DropdownPosition.right}
                    direction={DropdownDirection.up}
                    onSelect={this.onSelect}
                    toggle={<KebabToggle onToggle={isOpen => this.onToggle('toggle-id-4', isOpen)} id="toggle-id-4" />}
                    isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-4']}
                    isPlain
                    dropdownItems={notificationDrawerDropdownItems}
                    id="notification-4"
                  />
                </NotificationDrawerListItemHeader>
                <NotificationDrawerListItemBody timestamp="30 minutes ago">
                  This is a success notification description.
                </NotificationDrawerListItemBody>
              </NotificationDrawerListItem>
            </NotificationDrawerList>
          )}
          {!showNotifications && (
            <EmptyState variant={EmptyStateVariant.full}>
              <EmptyStateIcon icon={SearchIcon} />
              <Title headingLevel="h2" size="lg">
                No alerts found
              </Title>
              <EmptyStateBody>
                There are currently no alerts. There may be silenced critical alerts however.
              </EmptyStateBody>
              <EmptyStatePrimary>
                <Button variant="link">Action</Button>
              </EmptyStatePrimary>
            </EmptyState>
          )}
        </NotificationDrawerBody>
      </NotificationDrawer>
    );

    return (
      <React.Fragment>
        <Page
          header={Header}
          sidebar={Sidebar}
          isManagedSidebar
          notificationDrawer={notificationDrawer}
          onNotificationDrawerExpand={this.focusDrawer}
          isNotificationDrawerExpanded={isDrawerExpanded}
          skipToContent={PageSkipToContent}
          breadcrumb={PageBreadcrumb}
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
          <PageSection variant={PageSectionVariants.light} noPadding={true}>
            Panel section content
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```

### Grouped

When using the NotificationDrawerGroupList and related components, the function that is passed in to the `onNotificationDrawerExpand` prop on the Page component must also ensure the NotificationDrawer component only receives focus when it is initially opened. Otherwise any time a drawer group item is opened the NotificationDrawer component will receive focus, which would be unexpected behavior for users.

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
  Drawer,
  DrawerContent,
  DrawerContentBody,
  Dropdown,
  DropdownGroup,
  DropdownToggle,
  DropdownItem,
  DropdownSeparator,
  EmptyState,
  EmptyStateBody,
  EmptyStateIcon,
  EmptyStatePrimary,
  Gallery,
  GalleryItem,
  KebabToggle,
  Nav,
  NavItem,
  NavList,
  NotificationBadge,
  NotificationDrawer,
  NotificationDrawerBody,
  NotificationDrawerGroup,
  NotificationDrawerGroupList,
  NotificationDrawerHeader,
  NotificationDrawerList,
  NotificationDrawerListItem,
  NotificationDrawerListItemBody,
  NotificationDrawerListItemHeader,
  Page,
  PageSection,
  PageSectionVariants,
  PageSidebar,
  SkipToContent,
  Title,
  TextContent,
  Text,
  PageToggleButton,
  Masthead,
  MastheadMain,
  MastheadToggle,
  MastheadContent,
  MastheadBrand,
  Toolbar,
  ToolbarItem,
  ToolbarGroup,
  ToolbarContent
} from '@patternfly/react-core';
import { css } from '@patternfly/react-styles';
import BellIcon from '@patternfly/react-icons/dist/esm/icons/bell-icon';
import CogIcon from '@patternfly/react-icons/dist/esm/icons/cog-icon';
import AttentionBellIcon from '@patternfly/react-icons/dist/esm/icons/attention-bell-icon';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';
import HelpIcon from '@patternfly/react-icons/dist/esm/icons/help-icon';
import QuestionCircleIcon from '@patternfly/react-icons/dist/esm/icons/question-circle-icon';
import imgBrand from '@patternfly/react-core/src/components/Brand/examples/pfLogo.svg';
import imgAvatar from '@patternfly/react-core/src/components/Avatar/examples/avatarImg.svg';
import { Table, TableHeader, TableBody } from '@patternfly/react-table';

class GroupedNotificationDrawer extends React.Component {
  constructor(props) {
    super(props);
    this.drawerRef = React.createRef();
    this.state = {
      isDropdownOpen: false,
      isKebabDropdownOpen: false,
      activeItem: 0,
      isDrawerExpanded: false,
      firstDrawerGroupExpanded: false,
      secondDrawerGroupExpanded: true,
      thirdDrawerGroupExpanded: false,
      isActionsMenuOpen: null,
      showNotifications: true,
      isUnreadMap: {
        'group-1': {
          'notification-5': true,
          'notification-6': true
        },
        'group-2': {
          'notification-9': true,
          'notification-10': true
        },
        'group-3': null
      }
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

    this.onCloseNotificationDrawer = () => {
      this.setState(prevState => {
        return {
          isDrawerExpanded: !prevState.isDrawerExpanded
        };
      });
    };

    this.onToggle = (id, isOpen) => {
      this.setState({
        isActionsMenuOpen: { [id]: isOpen }
      });
    };

    this.onSelect = event => {
      this.setState({
        isActionsMenuOpen: null
      });
    };

    this.onListItemClick = (groupId, id) => {
      this.setState(prevState => {
        if (!prevState.isUnreadMap || !prevState.isUnreadMap[groupId]) return;
        console.log(prevState.isUnreadMap);
        prevState.isUnreadMap[groupId][id] = false;
        return {
          isUnreadMap: prevState.isUnreadMap
        };
      });
    };

    this.isUnread = (groupId, id) => {
      const { isUnreadMap } = this.state;
      return isUnreadMap && isUnreadMap[groupId] && isUnreadMap[groupId][id];
    };

    this.getNumberUnread = groupId => {
      const { isUnreadMap } = this.state;
      if (isUnreadMap === null) return 0;

      if (groupId) {
        if (isUnreadMap[groupId] === null) return 0;

        return Object.keys(isUnreadMap[groupId]).reduce((count, id) => {
          return isUnreadMap[groupId][id] ? count + 1 : count;
        }, 0);
      }

      return Object.keys(isUnreadMap).reduce((count, groupId) => {
        if (isUnreadMap[groupId] === null) return count;

        return Object.keys(isUnreadMap[groupId]).reduce((groupCount, id) => {
          return isUnreadMap[groupId][id] ? groupCount + 1 : groupCount;
        }, count);
      }, 0);
    };

    this.markAllRead = () => {
      this.setState({
        isUnreadMap: null
      });
    };

    this.showNotifications = showNotifications => {
      this.setState({
        isUnreadMap: null,
        showNotifications: showNotifications
      });
    };

    this.toggleFirstDrawer = (event, value) => {
      this.setState({
        firstDrawerGroupExpanded: value
      });
    };

    this.toggleSecondDrawer = (event, value) => {
      this.setState({
        secondDrawerGroupExpanded: value
      });
    };

    this.toggleThirdDrawer = (event, value) => {
      this.setState({
        thirdDrawerGroupExpanded: value
      });
    };

    this.focusDrawer = () => {
      // Prevent the NotificationDrawer from receiving focus if a drawer group item is opened
      if (!document.activeElement.closest(`.${this.drawerRef.current.className}`)) {
        const firstTabbableItem = this.drawerRef.current.querySelector('a, button');
        firstTabbableItem.focus();
      }
    };
  }

  render() {
    const {
      isDropdownOpen,
      isKebabDropdownOpen,
      activeItem,
      res,
      isDrawerExpanded,
      isActionsMenuOpen,
      isUnreadMap,
      showNotifications,
      firstDrawerGroupExpanded,
      secondDrawerGroupExpanded,
      thirdDrawerGroupExpanded
    } = this.state;

    const PageNav = (
      <Nav onSelect={this.onNavSelect} aria-label="Nav">
        <NavList>
          <NavItem itemId={0} isActive={activeItem === 0}>
            System Panel
          </NavItem>
          <NavItem itemId={1} isActive={activeItem === 1}>
            Policy
          </NavItem>
          <NavItem itemId={2} isActive={activeItem === 2}>
            Authentication
          </NavItem>
          <NavItem itemId={3} isActive={activeItem === 3}>
            Network Services
          </NavItem>
          <NavItem itemId={4} isActive={activeItem === 4}>
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
    const headerToolbar = (
      <Toolbar>
        <ToolbarContent>
          <ToolbarGroup spaceItems={{ default: 'spacerNone' }} alignment={{ default: 'alignRight' }}>
            <ToolbarGroup variant="icon-button-group">
              <ToolbarItem visibility={{ default: 'visible' }} isSelected={isDrawerExpanded}>
                <NotificationBadge
                  variant={this.getNumberUnread() === 0 ? 'read' : 'unread'}
                  onClick={this.onCloseNotificationDrawer}
                  aria-label="Notifications"
                >
                  <BellIcon />
                </NotificationBadge>
              </ToolbarItem>
              <ToolbarGroup
                variant="icon-button-group"
                visibility={{
                  default: 'hidden',
                  lg: 'visible'
                }} /** the settings and help icon buttons are only visible on desktop sizes and replaced by a kebab dropdown for other sizes */
              >
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
            </ToolbarGroup>
            <ToolbarGroup>
              <ToolbarItem
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
              </ToolbarItem>
              <ToolbarItem
                visibility={{ default: 'hidden', md: 'visible' }} /** this user dropdown is hidden on mobile sizes */
              >
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
            </ToolbarGroup>
          </ToolbarGroup>
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
            <Brand src={imgBrand} alt="Patternfly logo" />
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>{headerToolbar}</MastheadContent>
      </Masthead>
    );
    const Sidebar = <PageSidebar nav={PageNav} />;
    const pageId = 'main-content-page-layout-default-nav';
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

    const drawerContent = 'Panel content';

    const notificationDrawerDropdownItems = [
      <DropdownItem key="link">Link</DropdownItem>,
      <DropdownItem key="action" component="button">
        Action
      </DropdownItem>,
      <DropdownSeparator key="separator" />,
      <DropdownItem key="disabled link" isDisabled>
        Disabled Link
      </DropdownItem>
    ];

    const notificationDrawerActions = [
      <DropdownItem key="markAllRead" onClick={this.markAllRead} component="button">
        Mark all read
      </DropdownItem>,
      <DropdownItem key="clearAll" onClick={() => this.showNotifications(false)} component="button">
        Clear all
      </DropdownItem>,
      <DropdownItem key="unclearLast" onClick={() => this.showNotifications(true)} component="button">
        Unclear last
      </DropdownItem>,
      <DropdownItem key="settings" component="button">
        Settings
      </DropdownItem>
    ];

    const notificationDrawer = (
      <NotificationDrawer ref={this.drawerRef}>
        <NotificationDrawerHeader count={this.getNumberUnread()} onClose={this.onCloseNotificationDrawer}>
          <Dropdown
            onSelect={this.onSelect}
            toggle={<KebabToggle onToggle={isOpen => this.onToggle('toggle-id-0', isOpen)} id="toggle-id-0" />}
            isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-0']}
            isPlain
            dropdownItems={notificationDrawerActions}
            id="notification-0"
            position={DropdownPosition.right}
          />
        </NotificationDrawerHeader>
        <NotificationDrawerBody>
          {showNotifications && (
            <NotificationDrawerGroupList>
              <NotificationDrawerGroup
                title="First notification group"
                isExpanded={firstDrawerGroupExpanded}
                count={this.getNumberUnread('group-1')}
                onExpand={this.toggleFirstDrawer}
              >
                <NotificationDrawerList isHidden={!firstDrawerGroupExpanded}>
                  <NotificationDrawerListItem
                    variant="info"
                    onClick={() => this.onListItemClick('group-1', 'notification-5')}
                    isRead={!this.isUnread('group-1', 'notification-5')}
                  >
                    <NotificationDrawerListItemHeader
                      variant="info"
                      title="Unread info notification title"
                      srTitle="Info notification:"
                    >
                      <Dropdown
                        position={DropdownPosition.right}
                        onSelect={this.onSelect}
                        toggle={
                          <KebabToggle onToggle={isOpen => this.onToggle('toggle-id-5', isOpen)} id="toggle-id-5" />
                        }
                        isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-5']}
                        isPlain
                        dropdownItems={notificationDrawerDropdownItems}
                        id="notification-5"
                      />
                    </NotificationDrawerListItemHeader>
                    <NotificationDrawerListItemBody timestamp="5 minutes ago">
                      This is an info notification description.
                    </NotificationDrawerListItemBody>
                  </NotificationDrawerListItem>
                  <NotificationDrawerListItem
                    variant="danger"
                    onClick={() => this.onListItemClick('group-1', 'notification-6')}
                    isRead={!this.isUnread('group-1', 'notification-6')}
                  >
                    <NotificationDrawerListItemHeader
                      variant="danger"
                      title="Unread danger notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines."
                      srTitle="Danger notification:"
                    >
                      <Dropdown
                        position={DropdownPosition.right}
                        onSelect={this.onSelect}
                        toggle={
                          <KebabToggle onToggle={isOpen => this.onToggle('toggle-id-6', isOpen)} id="toggle-id-6" />
                        }
                        isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-6']}
                        isPlain
                        dropdownItems={notificationDrawerDropdownItems}
                        id="notification-6"
                      />
                    </NotificationDrawerListItemHeader>
                    <NotificationDrawerListItemBody timestamp="10 minutes ago">
                      This is a danger notification description. This is a long description to show how the title will
                      wrap if it is long and wraps to multiple lines.
                    </NotificationDrawerListItemBody>
                  </NotificationDrawerListItem>
                  <NotificationDrawerListItem
                    variant="warning"
                    onClick={() => this.onListItemClick('group-1', 'notification-7')}
                    isRead={!this.isUnread('group-1', 'notification-7')}
                  >
                    <NotificationDrawerListItemHeader
                      variant="warning"
                      title="Read warning notification title"
                      srTitle="Warning notification:"
                    >
                      <Dropdown
                        position={DropdownPosition.right}
                        onSelect={this.onSelect}
                        toggle={
                          <KebabToggle onToggle={isOpen => this.onToggle('toggle-id-7', isOpen)} id="toggle-id-7" />
                        }
                        isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-7']}
                        isPlain
                        dropdownItems={notificationDrawerDropdownItems}
                        id="notification-7"
                      />
                    </NotificationDrawerListItemHeader>
                    <NotificationDrawerListItemBody timestamp="20 minutes ago">
                      This is a warning notification description.
                    </NotificationDrawerListItemBody>
                  </NotificationDrawerListItem>
                  <NotificationDrawerListItem
                    variant="success"
                    onClick={() => this.onListItemClick('group-1', 'notification-8')}
                    isRead={!this.isUnread('group-1', 'notification-8')}
                  >
                    <NotificationDrawerListItemHeader
                      variant="success"
                      title="Read success notification title"
                      srTitle="Success notification:"
                    >
                      <Dropdown
                        position={DropdownPosition.right}
                        direction={DropdownDirection.up}
                        onSelect={this.onSelect}
                        toggle={
                          <KebabToggle onToggle={isOpen => this.onToggle('toggle-id-8', isOpen)} id="toggle-id-8" />
                        }
                        isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-8']}
                        isPlain
                        dropdownItems={notificationDrawerDropdownItems}
                        id="notification-8"
                      />
                    </NotificationDrawerListItemHeader>
                    <NotificationDrawerListItemBody timestamp="30 minutes ago">
                      This is a success notification description.
                    </NotificationDrawerListItemBody>
                  </NotificationDrawerListItem>
                </NotificationDrawerList>
              </NotificationDrawerGroup>
              <NotificationDrawerGroup
                title="Second notification group"
                isExpanded={secondDrawerGroupExpanded}
                count={this.getNumberUnread('group-2')}
                onExpand={this.toggleSecondDrawer}
              >
                <NotificationDrawerList isHidden={!secondDrawerGroupExpanded}>
                  <NotificationDrawerListItem
                    variant="info"
                    onClick={() => this.onListItemClick('group-2', 'notification-9')}
                    isRead={!this.isUnread('group-2', 'notification-9')}
                  >
                    <NotificationDrawerListItemHeader
                      variant="info"
                      title="Unread info notification title"
                      srTitle="Info notification:"
                    >
                      <Dropdown
                        position={DropdownPosition.right}
                        onSelect={this.onSelect}
                        toggle={
                          <KebabToggle onToggle={isOpen => this.onToggle('toggle-id-9', isOpen)} id="toggle-id-9" />
                        }
                        isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-9']}
                        isPlain
                        dropdownItems={notificationDrawerDropdownItems}
                        id="notification-9"
                      />
                    </NotificationDrawerListItemHeader>
                    <NotificationDrawerListItemBody timestamp="5 minutes ago">
                      This is an info notification description.
                    </NotificationDrawerListItemBody>
                  </NotificationDrawerListItem>
                  <NotificationDrawerListItem
                    variant="danger"
                    onClick={() => this.onListItemClick('group-2', 'notification-10')}
                    isRead={!this.isUnread('group-2', 'notification-10')}
                  >
                    <NotificationDrawerListItemHeader
                      variant="danger"
                      title="Unread danger notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines."
                      srTitle="Danger notification:"
                    >
                      <Dropdown
                        position={DropdownPosition.right}
                        onSelect={this.onSelect}
                        toggle={
                          <KebabToggle onToggle={isOpen => this.onToggle('toggle-id-10', isOpen)} id="toggle-id-10" />
                        }
                        isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-10']}
                        isPlain
                        dropdownItems={notificationDrawerDropdownItems}
                        id="notification-10"
                      />
                    </NotificationDrawerListItemHeader>
                    <NotificationDrawerListItemBody timestamp="10 minutes ago">
                      This is a danger notification description. This is a long description to show how the title will
                      wrap if it is long and wraps to multiple lines.
                    </NotificationDrawerListItemBody>
                  </NotificationDrawerListItem>
                  <NotificationDrawerListItem
                    variant="warning"
                    onClick={() => this.onListItemClick('group-2', 'notification-11')}
                    isRead={!this.isUnread('group-2', 'notification-11')}
                  >
                    <NotificationDrawerListItemHeader
                      variant="warning"
                      title="Read warning notification title"
                      srTitle="Warning notification:"
                    >
                      <Dropdown
                        position={DropdownPosition.right}
                        onSelect={this.onSelect}
                        toggle={
                          <KebabToggle onToggle={isOpen => this.onToggle('toggle-id-11', isOpen)} id="toggle-id-11" />
                        }
                        isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-11']}
                        isPlain
                        dropdownItems={notificationDrawerDropdownItems}
                        id="notification-11"
                      />
                    </NotificationDrawerListItemHeader>
                    <NotificationDrawerListItemBody timestamp="20 minutes ago">
                      This is a warning notification description.
                    </NotificationDrawerListItemBody>
                  </NotificationDrawerListItem>
                  <NotificationDrawerListItem
                    variant="success"
                    onClick={() => this.onListItemClick('group-2', 'notification-12')}
                    isRead={!this.isUnread('group-2', 'notification-12')}
                  >
                    <NotificationDrawerListItemHeader
                      variant="success"
                      title="Read success notification title"
                      srTitle="Success notification:"
                    >
                      <Dropdown
                        position={DropdownPosition.right}
                        direction={DropdownDirection.up}
                        onSelect={this.onSelect}
                        toggle={
                          <KebabToggle onToggle={isOpen => this.onToggle('toggle-id-12', isOpen)} id="toggle-id-12" />
                        }
                        isOpen={isActionsMenuOpen && isActionsMenuOpen['toggle-id-12']}
                        isPlain
                        dropdownItems={notificationDrawerDropdownItems}
                        id="notification-12"
                      />
                    </NotificationDrawerListItemHeader>
                    <NotificationDrawerListItemBody timestamp="30 minutes ago">
                      This is a success notification description.
                    </NotificationDrawerListItemBody>
                  </NotificationDrawerListItem>
                </NotificationDrawerList>
              </NotificationDrawerGroup>
              <NotificationDrawerGroup
                title="Third notification group"
                isExpanded={thirdDrawerGroupExpanded}
                count={this.getNumberUnread('group-3')}
                onExpand={this.toggleThirdDrawer}
              >
                <NotificationDrawerList isHidden={!thirdDrawerGroupExpanded}>
                  <EmptyState variant={EmptyStateVariant.full}>
                    <EmptyStateIcon icon={SearchIcon} />
                    <Title headingLevel="h2" size="lg">
                      No alerts found
                    </Title>
                    <EmptyStateBody>
                      There are currently no critical alerts firing. There may be firing alerts of other severities or
                      silenced critical alerts however.
                    </EmptyStateBody>
                    <EmptyStatePrimary>
                      <Button variant="link">Action</Button>
                    </EmptyStatePrimary>
                  </EmptyState>
                </NotificationDrawerList>
              </NotificationDrawerGroup>
            </NotificationDrawerGroupList>
          )}
          {!showNotifications && (
            <EmptyState variant={EmptyStateVariant.full}>
              <EmptyStateIcon icon={SearchIcon} />
              <Title headingLevel="h2" size="lg">
                No alerts found
              </Title>
              <EmptyStateBody>
                There are currently no alerts. There may be silenced critical alerts however.
              </EmptyStateBody>
              <EmptyStatePrimary>
                <Button variant="link">Action</Button>
              </EmptyStatePrimary>
            </EmptyState>
          )}
        </NotificationDrawerBody>
      </NotificationDrawer>
    );

    return (
      <React.Fragment>
        <Page
          header={Header}
          sidebar={Sidebar}
          isManagedSidebar
          notificationDrawer={notificationDrawer}
          isNotificationDrawerExpanded={isDrawerExpanded}
          onNotificationDrawerExpand={this.focusDrawer}
          skipToContent={PageSkipToContent}
          breadcrumb={PageBreadcrumb}
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
          <PageSection variant={PageSectionVariants.light} noPadding={true}>
            Panel section content
          </PageSection>
        </Page>
      </React.Fragment>
    );
  }
}
```
