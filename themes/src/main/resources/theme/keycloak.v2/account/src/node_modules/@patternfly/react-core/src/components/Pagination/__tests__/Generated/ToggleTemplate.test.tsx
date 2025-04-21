/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { ToggleTemplate } from '../../ToggleTemplate';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ToggleTemplate should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<ToggleTemplate firstIndex={0} lastIndex={0} itemCount={0} itemsTitle={"'items'"} />);
  expect(asFragment()).toMatchSnapshot();
});
