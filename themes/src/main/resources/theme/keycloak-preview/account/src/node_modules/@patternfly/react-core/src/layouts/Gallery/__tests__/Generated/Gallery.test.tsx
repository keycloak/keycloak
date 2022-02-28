/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Gallery } from '../../Gallery';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Gallery should match snapshot (auto-generated)', () => {
  const view = shallow(<Gallery children={<>ReactNode</>} className={"''"} gutter={null} />);
  expect(view).toMatchSnapshot();
});
