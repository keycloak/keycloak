/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataListItemCells } from '../../DataListItemCells';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListItemCells should match snapshot (auto-generated)', () => {
  const view = shallow(<DataListItemCells className={"''"} dataListCells={<div>ReactNode</div>} rowid={"''"} />);
  expect(view).toMatchSnapshot();
});
