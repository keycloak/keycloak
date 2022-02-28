/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { NavGroup } from '../../NavGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('NavGroup should match snapshot (auto-generated)', () => {
  const view = shallow(<NavGroup title={'string'} children={<>ReactNode</>} className={"''"} id={'string'} />);
  expect(view).toMatchSnapshot();
});
