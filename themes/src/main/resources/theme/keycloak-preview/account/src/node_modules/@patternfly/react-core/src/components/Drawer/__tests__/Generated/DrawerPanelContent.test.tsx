/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DrawerPanelContent } from '../../DrawerPanelContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DrawerPanelContent should match snapshot (auto-generated)', () => {
  const view = shallow(<DrawerPanelContent className={"''"} children={<div>ReactNode</div>} noPadding={false} />);
  expect(view).toMatchSnapshot();
});
