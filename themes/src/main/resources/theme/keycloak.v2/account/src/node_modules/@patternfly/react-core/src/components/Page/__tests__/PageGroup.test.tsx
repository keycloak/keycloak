import * as React from 'react';
import { render } from '@testing-library/react';
import { PageGroup } from '../PageGroup';

describe('page group', () => {
  test('Verify basic render', () => {
    const { asFragment } = render(<PageGroup>test</PageGroup>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify top sticky', () => {
    const { asFragment } = render(<PageGroup sticky="top">test</PageGroup>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify bottom sticky', () => {
    const { asFragment } = render(<PageGroup sticky="bottom">test</PageGroup>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify top shadow', () => {
    const { asFragment } = render(<PageGroup hasShadowTop>test</PageGroup>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify bottom shadow', () => {
    const { asFragment } = render(<PageGroup hasShadowBottom>test</PageGroup>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('Verify overflow scroll', () => {
    const { asFragment } = render(<PageGroup hasOverflowScroll>test</PageGroup>);
    expect(asFragment()).toMatchSnapshot();
  });
});
