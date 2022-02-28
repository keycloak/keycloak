import * as React from 'react';
import { mount } from 'enzyme';
import { PageSection, PageSectionTypes } from '../PageSection';

jest.mock('../Page');

test('Check page section with no padding example against snapshot', () => {
  const Section = <PageSection noPadding />;
  const view = mount(Section);
  expect(view).toMatchSnapshot();
});

test('Check page main nav section against snapshot', () => {
  const Section = <PageSection type={PageSectionTypes.nav} />;
  const view = mount(Section);
  expect(view).toMatchSnapshot();
});

test('Check page section with no padding on mobile example against snapshot', () => {
  const Section = <PageSection noPaddingMobile />;
  const view = mount(Section);
  expect(view).toMatchSnapshot();
});

test('Check page section with no fill example against snapshot', () => {
  const Section = <PageSection isFilled={false} />;
  const view = mount(Section);
  expect(view).toMatchSnapshot();
});

test('Check page section with fill example against snapshot', () => {
  const Section = <PageSection isFilled />;
  const view = mount(Section);
  expect(view).toMatchSnapshot();
});

test('Check page section with fill and no padding example against snapshot', () => {
  const Section = <PageSection isFilled noPadding />;
  const view = mount(Section);
  expect(view).toMatchSnapshot();
});
