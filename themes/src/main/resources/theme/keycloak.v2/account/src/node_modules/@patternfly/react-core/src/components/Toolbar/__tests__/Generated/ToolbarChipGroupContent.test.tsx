/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { ToolbarChipGroupContent } from '../../ToolbarChipGroupContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ToolbarChipGroupContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <ToolbarChipGroupContent
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
  expect(asFragment()).toMatchSnapshot();
});
