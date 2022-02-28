/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OverflowMenuContent } from '../../OverflowMenuContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OverflowMenuContent should match snapshot (auto-generated)', () => {
  const view = shallow(<OverflowMenuContent children={'any'} className={'string'} isPersistent={true} />);
  expect(view).toMatchSnapshot();
});
