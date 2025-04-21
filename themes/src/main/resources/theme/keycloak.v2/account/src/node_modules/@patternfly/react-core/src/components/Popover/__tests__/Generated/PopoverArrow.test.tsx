/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { PopoverArrow } from '../../PopoverArrow';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverArrow should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<PopoverArrow className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
