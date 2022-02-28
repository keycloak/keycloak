/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LoginMainBody } from '../../LoginMainBody';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginMainBody should match snapshot (auto-generated)', () => {
  const view = shallow(<LoginMainBody children={<>ReactNode</>} className={"''"} />);
  expect(view).toMatchSnapshot();
});
