/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ApplicationLauncherContent } from '../../ApplicationLauncherContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ApplicationLauncherContent should match snapshot (auto-generated)', () => {
  const view = shallow(<ApplicationLauncherContent children={<div>ReactNode</div>} />);
  expect(view).toMatchSnapshot();
});
