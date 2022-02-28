/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { DataToolbar } from '../../DataToolbar';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('DataToolbar should match snapshot (auto-generated)', () => {
  const view = shallow(
    <DataToolbar
      clearAllFilters={() => undefined as void}
      clearFiltersButtonText={'string'}
      collapseListedFiltersBreakpoint={'md'}
      isExpanded={true}
      toggleIsExpanded={() => undefined as void}
      className={'string'}
      children={<div>ReactNode</div>}
      id={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
