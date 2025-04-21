import * as React from 'react';
import { render } from '@testing-library/react';
import { PageNavigation } from '../PageNavigation';

describe('page navigation', () => {
  test('Verify basic render', () => {
    const { asFragment } = render(<PageNavigation>test</PageNavigation>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify limited width', () => {
    const { asFragment } = render(<PageNavigation isWidthLimited>test</PageNavigation>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify top sticky', () => {
    const { asFragment } = render(<PageNavigation sticky="top">test</PageNavigation>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify bottom sticky', () => {
    const { asFragment } = render(<PageNavigation sticky="bottom">test</PageNavigation>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify top shadow', () => {
    const { asFragment } = render(<PageNavigation hasShadowTop>test</PageNavigation>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify bottom shadow', () => {
    const { asFragment } = render(<PageNavigation hasShadowBottom>test</PageNavigation>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify overflow scroll', () => {
    const { asFragment } = render(<PageNavigation hasOverflowScroll>test</PageNavigation>);
    expect(asFragment()).toMatchSnapshot();
  });
});
