/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataListItemRow } from '../../DataListItemRow';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataListItemRow should match snapshot (auto-generated)', () => {
  const view = shallow(<DataListItemRow children={<div>ReactNode</div>} className={"''"} rowid={"''"} />);
  expect(view).toMatchSnapshot();
});
