/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { EmptyStatePrimary } from '../../EmptyStatePrimary';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('EmptyStatePrimary should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<EmptyStatePrimary className={"''"} children={<div>ReactNode</div>} />);
  expect(asFragment()).toMatchSnapshot();
});
