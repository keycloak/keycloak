/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { PopoverBody } from '../../PopoverBody';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverBody should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<PopoverBody id={'string'} children={<div>ReactNode</div>} />);
  expect(asFragment()).toMatchSnapshot();
});
