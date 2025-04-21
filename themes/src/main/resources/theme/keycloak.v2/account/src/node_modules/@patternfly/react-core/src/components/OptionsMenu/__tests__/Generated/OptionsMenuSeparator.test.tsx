/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { OptionsMenuSeparator } from '../../OptionsMenuSeparator';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OptionsMenuSeparator should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<OptionsMenuSeparator className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
