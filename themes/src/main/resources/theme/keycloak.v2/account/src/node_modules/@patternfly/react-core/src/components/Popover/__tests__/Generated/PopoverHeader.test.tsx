/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { PopoverHeader } from '../../PopoverHeader';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverHeader should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<PopoverHeader id={'string'} children={<div>ReactNode</div>} />);
  expect(asFragment()).toMatchSnapshot();
});
