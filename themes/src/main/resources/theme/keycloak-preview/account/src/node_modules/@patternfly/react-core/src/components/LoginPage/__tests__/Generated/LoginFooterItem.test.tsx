/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LoginFooterItem } from '../../LoginFooterItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginFooterItem should match snapshot (auto-generated)', () => {
  const view = shallow(<LoginFooterItem children={<>ReactNode</>} className={"''"} href={"'#'"} target={"'_blank'"} />);
  expect(view).toMatchSnapshot();
});
