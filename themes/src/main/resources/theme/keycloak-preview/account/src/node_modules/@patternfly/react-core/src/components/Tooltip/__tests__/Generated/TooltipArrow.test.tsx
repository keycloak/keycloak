/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { TooltipArrow } from '../../TooltipArrow';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TooltipArrow should match snapshot (auto-generated)', () => {
  const view = shallow(<TooltipArrow className={'string'} />);
  expect(view).toMatchSnapshot();
});
