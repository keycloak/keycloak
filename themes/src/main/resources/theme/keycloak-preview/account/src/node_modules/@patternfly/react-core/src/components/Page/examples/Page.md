---
title: 'Page'
section: components
cssPrefix: 'pf-c-page'
typescript: true
propComponents: ['Page', 'PageHeader', 'PageSidebar', 'PageSection']
---

import { Page, PageHeader, PageSidebar, PageSection, PageSectionVariants } from '@patternfly/react-core';

## Examples
```js title=Vertical-nav
import React from 'react';
import { Page, PageHeader, PageSidebar, PageSection, PageSectionVariants } from '@patternfly/react-core';

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
        toolbar="Toolbar"
        avatar=" | Avatar"
        showNavToggle
        isNavOpen={isNavOpen}
        onNavToggle={this.onNavToggle}
      />
    );
    const Sidebar = <PageSidebar nav="Navigation" isNavOpen={isNavOpen} theme="dark" />;

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

```js title=Horizontal-nav
import React from 'react';
import { Page, PageHeader, PageSidebar, PageSection, PageSectionVariants } from '@patternfly/react-core';

HorizontalPage = () => {
  const logoProps = {
    href: 'https://patternfly.org',
    onClick: () => console.log('clicked logo'),
    target: '_blank'
  };
  const Header = (
    <PageHeader logo="Logo" logoProps={logoProps} toolbar="Toolbar" avatar=" | Avatar" topNav="Navigation" />
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

```js title=Main-Section-Padding
import React from 'react';
import { Page, PageHeader, PageSidebar, PageSection, PageSectionVariants } from '@patternfly/react-core';

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
        toolbar="Toolbar"
        avatar=" | Avatar"
        showNavToggle
        isNavOpen={isNavOpen}
        onNavToggle={this.onNavToggle}
      />
    );
    const Sidebar = <PageSidebar nav="Navigation" isNavOpen={isNavOpen} theme="dark" />;

    return (
      <Page header={Header} sidebar={Sidebar}>
        <PageSection>Section with default padding</PageSection>
        <PageSection variant={PageSectionVariants.light} noPadding={true}>Section with no padding</PageSection>
        <PageSection noPaddingMobile={true}>Section with no padding on mobile only</PageSection>
      </Page>
    );
  }
}
```
```js title=With-or-without-fill
import React from 'react';
import { Page, PageHeader, PageSidebar, PageSection, PageSectionVariants } from '@patternfly/react-core';

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

    const logoProps = {
      href: 'https://patternfly.org',
      onClick: () => console.log('clicked logo'),
      target: '_blank'
    };
    const Header = (
      <PageHeader
        logo="Logo"
        logoProps={logoProps}
        toolbar="Toolbar"
        avatar=" | Avatar"
        showNavToggle
        isNavOpen={isNavOpen}
        onNavToggle={this.onNavToggle}
      />
    );
    const Sidebar = <PageSidebar nav="Navigation" isNavOpen={isNavOpen} theme="dark" />;

    return (
      <Page header={Header} sidebar={Sidebar}>
        <PageSection style={{height: '10em'}}>This section is set to the default fill variant</PageSection>
        <PageSection style={{height: '10em'}} isFilled={true}>This section fills the available space.</PageSection>
        <PageSection style={{height: '10em'}} isFilled={false}> This section does not fill the available space.</PageSection>
      </Page>
    );
  }
}
```
