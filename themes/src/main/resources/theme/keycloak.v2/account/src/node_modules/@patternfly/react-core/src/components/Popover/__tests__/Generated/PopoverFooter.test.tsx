/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { PopoverFooter } from '../../PopoverFooter';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverFooter should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<PopoverFooter className={"''"} children={<div>ReactNode</div>} />);
  expect(asFragment()).toMatchSnapshot();
});
