import * as React from 'react';
import { render } from '@testing-library/react';
import { ActionList } from '../ActionList';

describe('action list', () => {
  test('renders successfully', () => {
    const { asFragment } = render(<ActionList>test</ActionList>);
    expect(asFragment()).toMatchSnapshot();
  });
  test('isIconList flag adds modifier', () => {
    const { asFragment } = render(<ActionList isIconList>test</ActionList>);
    expect(asFragment()).toMatchSnapshot();
  });
});
