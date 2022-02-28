/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { PopoverDialog } from '../../PopoverDialog';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PopoverDialog should match snapshot (auto-generated)', () => {
  const view = shallow(<PopoverDialog position={'top'} className={'null'} children={<>ReactNode</>} />);
  expect(view).toMatchSnapshot();
});
