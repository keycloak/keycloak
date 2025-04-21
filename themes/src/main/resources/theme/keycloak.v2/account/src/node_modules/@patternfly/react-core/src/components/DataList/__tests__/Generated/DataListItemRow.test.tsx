/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { DataListItemRow } from '../../DataListItemRow';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListItemRow should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<DataListItemRow children={<div>ReactNode</div>} className={"''"} rowid={"''"} />);
  expect(asFragment()).toMatchSnapshot();
});
