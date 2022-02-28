/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LoginHeader } from '../../LoginHeader';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginHeader should match snapshot (auto-generated)', () => {
  const view = shallow(<LoginHeader children={<>ReactNode</>} className={"''"} headerBrand={null} />);
  expect(view).toMatchSnapshot();
});
