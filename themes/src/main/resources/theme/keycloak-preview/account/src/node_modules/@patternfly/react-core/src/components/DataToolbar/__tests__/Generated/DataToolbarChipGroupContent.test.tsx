/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataToolbarChipGroupContent } from '../../DataToolbarChipGroupContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataToolbarChipGroupContent should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataToolbarChipGroupContent
      className={'string'}
      isExpanded={true}
      chipGroupContentRef={{ current: document.createElement('div') }}
      clearAllFilters={() => undefined as void}
      showClearFiltersButton={true}
      clearFiltersButtonText={"'Clear all filters'"}
      numberOfFilters={42}
      collapseListedFiltersBreakpoint={'lg'}
    />
  );
  expect(view).toMatchSnapshot();
});
