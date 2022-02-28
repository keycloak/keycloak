/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Label } from '../../Label';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Label should match snapshot (auto-generated)', () => {
  const view = shallow(<Label children={<div>ReactNode</div>} className={"''"} isCompact={false} />);
  expect(view).toMatchSnapshot();
});
