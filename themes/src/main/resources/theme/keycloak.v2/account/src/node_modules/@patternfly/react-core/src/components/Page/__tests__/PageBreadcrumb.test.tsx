import * as React from 'react';
import { render } from '@testing-library/react';
import { PageBreadcrumb } from '../PageBreadcrumb';

describe('page breadcrumb', () => {
  test('Verify basic render', () => {
    const { asFragment } = render(<PageBreadcrumb>test</PageBreadcrumb>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify limited width', () => {
    const { asFragment } = render(<PageBreadcrumb isWidthLimited>test</PageBreadcrumb>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify top sticky', () => {
    const { asFragment } = render(<PageBreadcrumb sticky="top">test</PageBreadcrumb>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify bottom sticky', () => {
    const { asFragment } = render(<PageBreadcrumb sticky="bottom">test</PageBreadcrumb>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify top shadow', () => {
    const { asFragment } = render(<PageBreadcrumb hasShadowTop>test</PageBreadcrumb>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify bottom shadow', () => {
    const { asFragment } = render(<PageBreadcrumb hasShadowBottom>test</PageBreadcrumb>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify overflow scroll', () => {
    const { asFragment } = render(<PageBreadcrumb hasOverflowScroll>test</PageBreadcrumb>);
    expect(asFragment()).toMatchSnapshot();
  });
});
