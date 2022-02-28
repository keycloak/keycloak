/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataToolbarGroup } from '../../DataToolbarGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataToolbarGroup should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataToolbarGroup
      className={'string'}
      variant={'filter-group'}
      breakpointMods={[]}
      children={<div>ReactNode</div>}
      innerRef={{ current: document.createElement('div') }}
    />
  );
  expect(view).toMatchSnapshot();
});
