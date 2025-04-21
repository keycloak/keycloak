/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { EmptyStateBody } from '../../EmptyStateBody';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('EmptyStateBody should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<EmptyStateBody children={<div>ReactNode</div>} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
