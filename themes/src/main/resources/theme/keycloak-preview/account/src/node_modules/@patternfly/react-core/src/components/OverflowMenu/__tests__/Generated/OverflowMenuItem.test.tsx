/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OverflowMenuItem } from '../../OverflowMenuItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OverflowMenuItem should match snapshot (auto-generated)', () => {
  const view = shallow(<OverflowMenuItem children={'any'} className={'string'} isPersistent={false} />);
  expect(view).toMatchSnapshot();
});
