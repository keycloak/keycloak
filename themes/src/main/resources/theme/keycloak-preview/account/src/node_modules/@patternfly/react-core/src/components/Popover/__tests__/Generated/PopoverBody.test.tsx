/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { PopoverBody } from '../../PopoverBody';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverBody should match snapshot (auto-generated)', () => {
  const view = shallow(<PopoverBody id={'string'} children={<div>ReactNode</div>} />);
  expect(view).toMatchSnapshot();
});
