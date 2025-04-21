/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { AlertIcon } from '../../AlertIcon';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AlertIcon should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<AlertIcon variant={'success'} className={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
