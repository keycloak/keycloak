---
id: Page
section: components
cssPrefix: pf-c-page
propComponents:
  [
    'Page',
    'PageHeader',
    'PageHeaderTools',
    'PageHeaderToolsGroup',
    'PageHeaderToolsItem',
    'PageSidebar',
    'PageSection',
    'PageGroup',
    'PageBreadcrumb',
    'PageNavigation',
    'PageToggleButton',
  ]
---

import BarsIcon from '@patternfly/react-icons/dist/js/icons/bars-icon';

## Examples

### Vertical nav

```js
import React from 'react';
import {
  Page,
  Masthead,
  MastheadToggle,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  PageSidebar,
  PageSection,
  PageSectionVariants,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarItem
} from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';

class VerticalPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isNavOpen: true
    };
    this.onNavToggle = () => {
      this.setState({
        isNavOpen: !this.state.isNavOpen
      });
    };
  }

  render() {
    const { isNavOpen } = this.state;

    const headerToolbar = (
      <Toolbar id="toolbar">
        <ToolbarContent>
          <ToolbarItem>header-tools</ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    );

    const Header = (
      <Masthead>
        <MastheadToggle>
          <PageToggleButton
            variant="plain"
            aria-label="Global navigation"
            isNavOpen={isNavOpen}
            onNavToggle={this.onNavToggle}
          >
            <BarsIcon />
          </PageToggleButton>
        </MastheadToggle>
        <MastheadMain>
          <MastheadBrand href="https://patternfly.org" onClick={() => console.log('clicked logo')} target="_blank">
            Logo
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>{headerToolbar}</MastheadContent>
      </Masthead>
    );
    const Sidebar = <PageSidebar nav="Navigation" isNavOpen={isNavOpen} />;

    return (
      <Page header={Header} sidebar={Sidebar}>
        <PageSection variant={PageSectionVariants.darker}>Section with darker background</PageSection>
        <PageSection variant={PageSectionVariants.dark}>Section with dark background</PageSection>
        <PageSection variant={PageSectionVariants.light}>Section with light background</PageSection>
      </Page>
    );
  }
}
```

### Horizontal nav

```js
import React from 'react';
import {
  Page,
  Masthead,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  PageSection,
  PageSectionVariants,
  Toolbar,
  ToolbarContent,
  ToolbarItem
} from '@patternfly/react-core';

HorizontalPage = () => {
  const headerToolbar = (
    <Toolbar id="toolbar">
      <ToolbarContent>
        <ToolbarItem>Navigation</ToolbarItem>
        <ToolbarItem>header-tools</ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );

  const Header = (
    <Masthead inset={{ default: 'insetXs' }}>
      <MastheadMain>
        <MastheadBrand href="https://patternfly.org" onClick={() => console.log('clicked logo')} target="_blank">
          Logo
        </MastheadBrand>
      </MastheadMain>
      <MastheadContent>{headerToolbar}</MastheadContent>
    </Masthead>
  );

  return (
    <Page header={Header}>
      <PageSection variant={PageSectionVariants.darker}>Section with darker background</PageSection>
      <PageSection variant={PageSectionVariants.dark}>Section with dark background</PageSection>
      <PageSection variant={PageSectionVariants.light}>Section with light background</PageSection>
    </Page>
  );
};
```

### Tertiary nav

```js
import React from 'react';
import {
  Page,
  Masthead,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  PageSection,
  PageSectionVariants,
  Toolbar,
  ToolbarContent,
  ToolbarItem
} from '@patternfly/react-core';

TertiaryPage = () => {
  const headerToolbar = (
    <Toolbar id="toolbar">
      <ToolbarContent>
        <ToolbarItem>header-tools</ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );

  const Header = (
    <Masthead display={{ default: 'stack' }} inset={{ default: 'insetXs' }}>
      <MastheadMain>
        <MastheadBrand href="https://patternfly.org" onClick={() => console.log('clicked logo')} target="_blank">
          Logo
        </MastheadBrand>
      </MastheadMain>
      <MastheadContent>{headerToolbar}</MastheadContent>
    </Masthead>
  );

  return (
    <Page header={Header} tertiaryNav="Navigation">
      <PageSection variant={PageSectionVariants.darker}>Section with darker background</PageSection>
      <PageSection variant={PageSectionVariants.dark}>Section with dark background</PageSection>
      <PageSection variant={PageSectionVariants.light}>Section with light background</PageSection>
    </Page>
  );
};
```

### With or without fill

```js
import React from 'react';
import {
  Page,
  Masthead,
  MastheadToggle,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  PageSidebar,
  PageSection,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarItem
} from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';

class FillPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isNavOpen: true
    };
    this.onNavToggle = () => {
      this.setState({
        isNavOpen: !this.state.isNavOpen
      });
    };
  }

  render() {
    const { isNavOpen } = this.state;

    const headerToolbar = (
      <Toolbar id="toolbar">
        <ToolbarContent>
          <ToolbarItem>header-tools</ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    );

    const Header = (
      <Masthead>
        <MastheadToggle>
          <PageToggleButton
            variant="plain"
            aria-label="Global navigation"
            isNavOpen={isNavOpen}
            onNavToggle={this.onNavToggle}
          >
            <BarsIcon />
          </PageToggleButton>
        </MastheadToggle>
        <MastheadMain>
          <MastheadBrand href="https://patternfly.org" onClick={() => console.log('clicked logo')} target="_blank">
            Logo
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>{headerToolbar}</MastheadContent>
      </Masthead>
    );
    const Sidebar = <PageSidebar nav="Navigation" isNavOpen={isNavOpen} />;

    return (
      <Page header={Header} sidebar={Sidebar}>
        <PageSection>A default page section</PageSection>
        <PageSection isFilled={true}>This section fills the available space.</PageSection>
        <PageSection isFilled={false}>
          This section is set to not fill the available space, since the last page section is set to fill the available
          space by default.
        </PageSection>
      </Page>
    );
  }
}
```

### Main section padding

```js
import React from 'react';
import {
  Page,
  Masthead,
  MastheadToggle,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  PageSidebar,
  PageSection,
  PageSectionVariants,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarItem
} from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';

class VerticalPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isNavOpen: true
    };
    this.onNavToggle = () => {
      this.setState({
        isNavOpen: !this.state.isNavOpen
      });
    };
  }

  render() {
    const { isNavOpen } = this.state;

    const headerToolbar = (
      <Toolbar id="toolbar">
        <ToolbarContent>
          <ToolbarItem>header-tools</ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    );

    const Header = (
      <Masthead>
        <MastheadToggle>
          <PageToggleButton
            variant="plain"
            aria-label="Global navigation"
            isNavOpen={isNavOpen}
            onNavToggle={this.onNavToggle}
          >
            <BarsIcon />
          </PageToggleButton>
        </MastheadToggle>
        <MastheadMain>
          <MastheadBrand href="https://patternfly.org" onClick={() => console.log('clicked logo')} target="_blank">
            Logo
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>{headerToolbar}</MastheadContent>
      </Masthead>
    );
    const Sidebar = <PageSidebar nav="Navigation" isNavOpen={isNavOpen} />;

    return (
      <Page header={Header} sidebar={Sidebar}>
        <PageSection>Section with default padding</PageSection>
        <PageSection variant={PageSectionVariants.light} padding={{ default: 'noPadding' }}>
          Section with no padding
        </PageSection>
        <PageSection padding={{ default: 'noPadding', md: 'padding', lg: 'padding' }}>
          Section with padding only on medium/large
        </PageSection>
        <PageSection variant={PageSectionVariants.light} padding={{ md: 'noPadding' }}>
          Section with no padding on medium
        </PageSection>
      </Page>
    );
  }
}
```

### Uncontrolled nav

```js
import React from 'react';
import {
  Page,
  Masthead,
  MastheadToggle,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  PageSidebar,
  PageSection,
  PageSectionVariants,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarItem
} from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';

class UncontrolledNavPage extends React.Component {
  render() {
    const headerToolbar = (
      <Toolbar id="toolbar">
        <ToolbarContent>
          <ToolbarItem>header-tools</ToolbarItem>
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
          <MastheadBrand href="https://patternfly.org" onClick={() => console.log('clicked logo')} target="_blank">
            Logo
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>{headerToolbar}</MastheadContent>
      </Masthead>
    );
    const Sidebar = <PageSidebar nav="Navigation" />;

    return (
      <Page isManagedSidebar header={Header} sidebar={Sidebar}>
        <PageSection variant={PageSectionVariants.darker}>Section with darker background</PageSection>
        <PageSection variant={PageSectionVariants.dark}>Section with dark background</PageSection>
        <PageSection variant={PageSectionVariants.light}>Section with light background</PageSection>
      </Page>
    );
  }
}
```

### Group section

```js
import React from 'react';
import {
  Page,
  Masthead,
  MastheadToggle,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  PageSidebar,
  PageSection,
  PageGroup,
  PageBreadcrumb,
  PageNavigation,
  PageSectionVariants,
  PageToggleButton,
  Breadcrumb,
  BreadcrumbItem,
  Nav,
  NavList,
  NavItem,
  Toolbar,
  ToolbarContent,
  ToolbarItem
} from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';

class GroupPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isNavOpen: true
    };
    this.onNavToggle = () => {
      this.setState({
        isNavOpen: !this.state.isNavOpen
      });
    };
  }

  render() {
    const { isNavOpen } = this.state;

    const headerToolbar = (
      <Toolbar id="toolbar">
        <ToolbarContent>
          <ToolbarItem>header-tools</ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    );

    const Header = (
      <Masthead>
        <MastheadToggle>
          <PageToggleButton
            variant="plain"
            aria-label="Global navigation"
            isNavOpen={isNavOpen}
            onNavToggle={this.onNavToggle}
          >
            <BarsIcon />
          </PageToggleButton>
        </MastheadToggle>
        <MastheadMain>
          <MastheadBrand href="https://patternfly.org" onClick={() => console.log('clicked logo')} target="_blank">
            Logo
          </MastheadBrand>
        </MastheadMain>
        <MastheadContent>{headerToolbar}</MastheadContent>
      </Masthead>
    );
    const Sidebar = <PageSidebar nav="Navigation" isNavOpen={isNavOpen} />;

    return (
      <Page header={Header} sidebar={Sidebar}>
        <PageGroup>
          <PageNavigation>
            <Nav aria-label="Nav" variant="tertiary">
              <NavList>
                <NavItem itemId={0} isActive>
                  System panel
                </NavItem>
                <NavItem itemId={1}>Policy</NavItem>
                <NavItem itemId={2}>Authentication</NavItem>
                <NavItem itemId={3}>Network services</NavItem>
                <NavItem itemId={4}>Server</NavItem>
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
          <PageSection variant={PageSectionVariants.light}>Grouped section</PageSection>
        </PageGroup>
        <PageSection variant={PageSectionVariants.dark}>Section 1</PageSection>
        <PageSection variant={PageSectionVariants.dark}>Section 2</PageSection>
      </Page>
    );
  }
}
```

### Vertical nav using PageHeader component

This example is provided becuase PageHeader and PageHeaderTools are still in use; however, going forward Masthead and Toolbar should be used to make headers rather than PageHeader and PageHeaderTools.

```js
import React from 'react';
import {
  Page,
  PageHeader,
  PageHeaderTools,
  PageSidebar,
  PageSection,
  PageSectionVariants
} from '@patternfly/react-core';

class VerticalPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isNavOpen: true
    };
    this.onNavToggle = () => {
      this.setState({
        isNavOpen: !this.state.isNavOpen
      });
    };
  }

  render() {
    const { isNavOpen } = this.state;

    const logoProps = {
      href: 'https://patternfly.org',
      onClick: () => console.log('clicked logo'),
      target: '_blank'
    };
    const Header = (
      <PageHeader
        logo="Logo"
        logoProps={logoProps}
        headerTools={<PageHeaderTools>header-tools</PageHeaderTools>}
        showNavToggle
        isNavOpen={isNavOpen}
        onNavToggle={this.onNavToggle}
      />
    );
    const Sidebar = <PageSidebar nav="Navigation" isNavOpen={isNavOpen} />;

    return (
      <Page header={Header} sidebar={Sidebar}>
        <PageSection variant={PageSectionVariants.darker}>Section with darker background</PageSection>
        <PageSection variant={PageSectionVariants.dark}>Section with dark background</PageSection>
        <PageSection variant={PageSectionVariants.light}>Section with light background</PageSection>
      </Page>
    );
  }
}
```

### Centered section

```js
import React from 'react';
import {
  Page,
  Masthead,
  MastheadToggle,
  MastheadMain,
  MastheadBrand,
  MastheadContent,
  PageSidebar,
  PageSection,
  PageSectionVariants,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Card,
  CardBody
} from '@patternfly/react-core';
import BarsIcon from '@patternfly/react-icons/dist/esm/icons/bars-icon';

CenterAlignedPageSection = () => {
  const headerToolbar = (
    <Toolbar id="toolbar">
      <ToolbarContent>
        <ToolbarItem>header-tools</ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );

  const Header = (
    <Masthead>
      <MastheadToggle>
        <PageToggleButton variant="plain" aria-label="Global navigation" onNavToggle={this.onNavToggle}>
          <BarsIcon />
        </PageToggleButton>
      </MastheadToggle>
      <MastheadMain>
        <MastheadBrand href="https://patternfly.org" onClick={() => console.log('clicked logo')} target="_blank">
          Logo
        </MastheadBrand>
      </MastheadMain>
      <MastheadContent>{headerToolbar}</MastheadContent>
    </Masthead>
  );

  return (
    <Page header={Header}>
      <PageSection isWidthLimited isCenterAligned>
        <Card>
          <CardBody>
            When a width limited page section is wider than the value of
            <code>--pf-c-page--section--m-limit-width--MaxWidth</code>, the section will be centered in the main section.
            <br />
            <br />
            The content in this example is placed in a card to better illustrate how the section behaves when it is
            centered. A card is not required to center a page section.
          </CardBody>
        </Card>
      </PageSection>
    </Page>
  );
};
```
