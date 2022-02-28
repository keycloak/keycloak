/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OverflowMenuControl } from '../../OverflowMenuControl';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OverflowMenuControl should match snapshot (auto-generated)', () => {
  const view = shallow(<OverflowMenuControl children={'any'} className={'string'} hasAdditionalOptions={true} />);
  expect(view).toMatchSnapshot();
});
