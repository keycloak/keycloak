import * as React from 'react';
import { mount } from 'enzyme';
import { Page } from '../Page';
import { PageHeader } from '../PageHeader';
import { PageSidebar } from '../PageSidebar';
import { PageSection } from '../PageSection';
import { Breadcrumb, BreadcrumbItem } from '../../Breadcrumb';
import { SkipToContent } from '../../SkipToContent';

const props = {
  'aria-label': 'Page layout',
  id: 'PageId',
  className: 'my-page-class'
};

test('Check page vertical layout example against snapshot', () => {
  const Header = <PageHeader logo="Logo" toolbar="Toolbar" avatar=" | Avatar" onNavToggle={() => undefined} />;
  const Sidebar = <PageSidebar nav="Navigation" isNavOpen />;
  const view = mount(
    <Page {...props} header={Header} sidebar={Sidebar}>
      <PageSection variant="default">Section with default background</PageSection>
      <PageSection variant="light">Section with light background</PageSection>
      <PageSection variant="dark">Section with dark background</PageSection>
      <PageSection variant="darker">Section with darker background</PageSection>
    </Page>
  );
  expect(view).toMatchSnapshot();
});

test('Check dark page against snapshot', () => {
  const Header = <PageHeader logo="Logo" toolbar="Toolbar" avatar=" | Avatar" onNavToggle={() => undefined} />;
  const Sidebar = <PageSidebar nav="Navigation" isNavOpen theme="dark" />;
  const view = mount(
    <Page {...props} header={Header} sidebar={Sidebar}>
      <PageSection variant="default">Section with default background</PageSection>
      <PageSection variant="light">Section with light background</PageSection>
      <PageSection variant="dark">Section with dark background</PageSection>
      <PageSection variant="darker">Section with darker background</PageSection>
    </Page>
  );
  expect(view).toMatchSnapshot();
});

test('Check page horizontal layout example against snapshot', () => {
  const Header = <PageHeader logo="Logo" toolbar="Toolbar" avatar=" | Avatar" topNav="Navigation" />;
  const Sidebar = <PageSidebar isNavOpen />;
  const view = mount(
    <Page {...props} header={Header} sidebar={Sidebar}>
      <PageSection variant="default">Section with default background</PageSection>
      <PageSection variant="light">Section with light background</PageSection>
      <PageSection variant="dark">Section with dark background</PageSection>
      <PageSection variant="darker">Section with darker background</PageSection>
    </Page>
  );
  expect(view).toMatchSnapshot();
});

test('Check page to verify breadcrumb is created', () => {
  const Header = <PageHeader logo="Logo" toolbar="Toolbar" avatar=" | Avatar" topNav="Navigation" />;
  const Sidebar = <PageSidebar isNavOpen />;
  const PageBreadcrumb = () => (
    <Breadcrumb>
      <BreadcrumbItem>Section Home</BreadcrumbItem>
      <BreadcrumbItem to="#">Section Title</BreadcrumbItem>
      <BreadcrumbItem to="#">Section Title</BreadcrumbItem>
      <BreadcrumbItem to="#" isActive>
        Section Landing
      </BreadcrumbItem>
    </Breadcrumb>
  );
  const view = mount(
    <Page {...props} header={Header} sidebar={Sidebar} breadcrumb={<PageBreadcrumb />}>
      <PageSection variant="default">Section with default background</PageSection>
      <PageSection variant="light">Section with light background</PageSection>
      <PageSection variant="dark">Section with dark background</PageSection>
      <PageSection variant="darker">Section with darker background</PageSection>
    </Page>
  );
  expect(view).toMatchSnapshot();
  expect(view.find('.pf-c-page__main').getDOMNode().id).toBe('');
});

test('Check page to verify skip to content points to main content region', () => {
  const Header = <PageHeader logo="Logo" toolbar="Toolbar" avatar=" | Avatar" topNav="Navigation" />;
  const Sidebar = <PageSidebar isNavOpen />;
  const PageBreadcrumb = (
    <Breadcrumb>
      <BreadcrumbItem>Section Home</BreadcrumbItem>
      <BreadcrumbItem to="#">Section Title</BreadcrumbItem>
      <BreadcrumbItem to="#">Section Title</BreadcrumbItem>
      <BreadcrumbItem to="#" isActive>
        Section Landing
      </BreadcrumbItem>
    </Breadcrumb>
  );
  const mainId = 'main-content-page-layout-test-nav';
  const PageSkipToContent = <SkipToContent href={`#${mainId}`}>Skip to Content</SkipToContent>;
  const view = mount(
    <Page
      {...props}
      header={Header}
      sidebar={Sidebar}
      breadcrumb={PageBreadcrumb}
      skipToContent={PageSkipToContent}
      mainContainerId={mainId}
    >
      <PageSection variant="default">Section with default background</PageSection>
      <PageSection variant="light">Section with light background</PageSection>
      <PageSection variant="dark">Section with dark background</PageSection>
      <PageSection variant="darker">Section with darker background</PageSection>
    </Page>
  );
  expect(view).toMatchSnapshot();
  expect(view.find('.pf-c-page').getDOMNode().id).toBe(props.id);
  expect(view.find('.pf-c-page__main').getDOMNode().id).toBe(mainId);
});
