/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { ToolbarContent } from '../../ToolbarContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ToolbarContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <ToolbarContent
      className={'string'}
      children={<div>ReactNode</div>}
      isExpanded={false}
      clearAllFilters={() => undefined as void}
      showClearFiltersButton={false}
      clearFiltersButtonText={'string'}
      toolbarId={'string'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
