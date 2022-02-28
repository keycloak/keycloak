/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataToolbarContent } from '../../DataToolbarContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataToolbarContent should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataToolbarContent
      className={'string'}
      breakpointMods={[]}
      children={<div>ReactNode</div>}
      isExpanded={false}
      clearAllFilters={() => undefined as void}
      showClearFiltersButton={false}
      clearFiltersButtonText={'string'}
      toolbarId={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
