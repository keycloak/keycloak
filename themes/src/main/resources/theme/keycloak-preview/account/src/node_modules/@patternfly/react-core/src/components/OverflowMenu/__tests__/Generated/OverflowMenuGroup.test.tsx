/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OverflowMenuGroup } from '../../OverflowMenuGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OverflowMenuGroup should match snapshot (auto-generated)', () => {
  const view = shallow(
    <OverflowMenuGroup children={'any'} className={'string'} isPersistent={false} groupType={'button'} />
  );
  expect(view).toMatchSnapshot();
});
