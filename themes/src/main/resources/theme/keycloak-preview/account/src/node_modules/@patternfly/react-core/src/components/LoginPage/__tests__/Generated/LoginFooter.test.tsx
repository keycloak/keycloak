/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LoginFooter } from '../../LoginFooter';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginFooter should match snapshot (auto-generated)', () => {
  const view = shallow(<LoginFooter children={<>ReactNode</>} className={"''"} />);
  expect(view).toMatchSnapshot();
});
