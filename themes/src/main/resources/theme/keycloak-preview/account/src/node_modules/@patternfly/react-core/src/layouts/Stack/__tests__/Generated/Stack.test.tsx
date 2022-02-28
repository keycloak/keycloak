/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Stack } from '../../Stack';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Stack should match snapshot (auto-generated)', () => {
  const view = shallow(<Stack gutter={null} children={<>ReactNode</>} className={"''"} component={'div'} />);
  expect(view).toMatchSnapshot();
});
