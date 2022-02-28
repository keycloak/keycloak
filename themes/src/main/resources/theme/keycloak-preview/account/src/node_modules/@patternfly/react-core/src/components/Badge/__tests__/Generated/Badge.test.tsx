/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Badge } from '../../Badge';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Badge should match snapshot (auto-generated)', () => {
  const view = shallow(<Badge isRead={false} children={''} className={"''"} />);
  expect(view).toMatchSnapshot();
});
