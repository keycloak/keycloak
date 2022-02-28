/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ToolbarSection } from '../../ToolbarSection';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ToolbarSection should match snapshot (auto-generated)', () => {
  const view = shallow(<ToolbarSection children={<>ReactNode</>} className={'null'} aria-label={'string'} />);
  expect(view).toMatchSnapshot();
});
