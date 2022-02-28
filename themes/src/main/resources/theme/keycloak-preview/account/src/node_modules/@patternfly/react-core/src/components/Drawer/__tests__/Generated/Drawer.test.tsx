/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Drawer } from '../../Drawer';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Drawer should match snapshot (auto-generated)', () => {
  const view = shallow(<Drawer className={"''"} children={<div>ReactNode</div>} isExpanded={false} isInline={false} />);
  expect(view).toMatchSnapshot();
});
