/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LoginMainHeader } from '../../LoginMainHeader';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginMainHeader should match snapshot (auto-generated)', () => {
  const view = shallow(<LoginMainHeader children={<>ReactNode</>} className={"''"} title={"''"} subtitle={"''"} />);
  expect(view).toMatchSnapshot();
});
