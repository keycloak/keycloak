/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { AlertGroupInline } from '../../AlertGroupInline';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AlertGroupInline should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<AlertGroupInline />);
  expect(asFragment()).toMatchSnapshot();
});
