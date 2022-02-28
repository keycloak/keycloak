/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { PopoverCloseButton } from '../../PopoverCloseButton';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverCloseButton should match snapshot (auto-generated)', () => {
  const view = shallow(<PopoverCloseButton onClose={() => undefined as void} aria-label={'string'} />);
  expect(view).toMatchSnapshot();
});
