/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { EmptyStatePrimary } from '../../EmptyStatePrimary';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('EmptyStatePrimary should match snapshot (auto-generated)', () => {
  const view = shallow(<EmptyStatePrimary className={"''"} children={<div>ReactNode</div>} />);
  expect(view).toMatchSnapshot();
});
