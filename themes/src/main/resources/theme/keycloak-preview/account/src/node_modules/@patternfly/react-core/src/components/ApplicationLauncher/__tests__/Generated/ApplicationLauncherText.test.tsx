/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ApplicationLauncherText } from '../../ApplicationLauncherText';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ApplicationLauncherText should match snapshot (auto-generated)', () => {
  const view = shallow(<ApplicationLauncherText children={<div>ReactNode</div>} className={"''"} />);
  expect(view).toMatchSnapshot();
});
