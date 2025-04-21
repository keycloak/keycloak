/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { DataListItemCells } from '../../DataListItemCells';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListItemCells should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <DataListItemCells className={"''"} dataListCells={<div>ReactNode</div>} rowid={"''"} />
  );
  expect(asFragment()).toMatchSnapshot();
});
