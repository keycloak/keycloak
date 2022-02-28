/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ToolbarGroup } from '../../ToolbarGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ToolbarGroup should match snapshot (auto-generated)', () => {
  const view = shallow(<ToolbarGroup children={<>ReactNode</>} className={'null'} />);
  expect(view).toMatchSnapshot();
});
