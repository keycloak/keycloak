/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { PopoverContent } from '../../PopoverContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverContent should match snapshot (auto-generated)', () => {
  const view = shallow(<PopoverContent className={'null'} children={<div>ReactNode</div>} />);
  expect(view).toMatchSnapshot();
});
