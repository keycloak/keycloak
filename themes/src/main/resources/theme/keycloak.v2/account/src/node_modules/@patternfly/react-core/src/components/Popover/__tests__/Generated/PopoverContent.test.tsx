/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { PopoverContent } from '../../PopoverContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<PopoverContent className={'null'} children={<div>ReactNode</div>} />);
  expect(asFragment()).toMatchSnapshot();
});
