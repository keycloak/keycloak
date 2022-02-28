/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataToolbarItem } from '../../DataToolbarItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataToolbarItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataToolbarItem
      className={'string'}
      variant={'separator'}
      breakpointMods={[]}
      id={'string'}
      children={<div>ReactNode</div>}
    />
  );
  expect(view).toMatchSnapshot();
});
