import * as React from 'react';
import { render } from '@testing-library/react';
import { PageSection, PageSectionTypes } from '../PageSection';

jest.mock('../Page');

test('Check page section with no padding example against snapshot', () => {
  const Section = <PageSection padding={{ default: 'noPadding' }} />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check page section with limited width', () => {
  const Section = <PageSection isWidthLimited />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check page section with center alignment', () => {
  const Section = <PageSection isWidthLimited isCenterAligned />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check page main tabs section against snapshot', () => {
  const Section = <PageSection type={PageSectionTypes.tabs} />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check page main nav section against snapshot', () => {
  const Section = <PageSection type={PageSectionTypes.nav} />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check page main subnav section against snapshot', () => {
  const Section = <PageSection type={PageSectionTypes.subNav} />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check page main breadcrumb section against snapshot', () => {
  const Section = <PageSection type={PageSectionTypes.breadcrumb} />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check page section with no fill example against snapshot', () => {
  const Section = <PageSection isFilled={false} />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check page section with fill example against snapshot', () => {
  const Section = <PageSection isFilled />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Check page section with fill and no padding example against snapshot', () => {
  const Section = <PageSection isFilled padding={{ default: 'noPadding' }} />;
  const { asFragment } = render(Section);
  expect(asFragment()).toMatchSnapshot();
});

test('Verify page section top sticky', () => {
  const { asFragment } = render(<PageSection sticky="top">test</PageSection>);
  expect(asFragment()).toMatchSnapshot();
});

test('Verify page section bottom sticky', () => {
  const { asFragment } = render(<PageSection sticky="bottom">test</PageSection>);
  expect(asFragment()).toMatchSnapshot();
});

test('Verify page section top shadow', () => {
  const { asFragment } = render(<PageSection hasShadowTop>test</PageSection>);
  expect(asFragment()).toMatchSnapshot();
});

test('Verify page section bottom shadow', () => {
  const { asFragment } = render(<PageSection hasShadowBottom>test</PageSection>);
  expect(asFragment()).toMatchSnapshot();
});

test('Verify page section overflow scroll', () => {
  const { asFragment } = render(<PageSection hasOverflowScroll>test</PageSection>);
  expect(asFragment()).toMatchSnapshot();
});
