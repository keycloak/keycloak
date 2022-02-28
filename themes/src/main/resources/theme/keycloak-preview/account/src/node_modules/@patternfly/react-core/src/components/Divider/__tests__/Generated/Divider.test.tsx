/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Divider } from '../../Divider';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Divider should match snapshot (auto-generated)', () => {
  const view = shallow(<Divider className={"''"} component={'hr'} />);
  expect(view).toMatchSnapshot();
});
