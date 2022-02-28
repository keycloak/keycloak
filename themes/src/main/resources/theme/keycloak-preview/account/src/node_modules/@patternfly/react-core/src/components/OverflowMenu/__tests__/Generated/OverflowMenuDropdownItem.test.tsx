/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OverflowMenuDropdownItem } from '../../OverflowMenuDropdownItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OverflowMenuDropdownItem should match snapshot (auto-generated)', () => {
  const view = shallow(<OverflowMenuDropdownItem children={'any'} isShared={false} />);
  expect(view).toMatchSnapshot();
});
