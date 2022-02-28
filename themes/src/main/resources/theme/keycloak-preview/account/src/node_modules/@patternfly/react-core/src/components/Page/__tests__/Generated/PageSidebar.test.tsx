/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { PageSidebar } from '../../PageSidebar';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('PageSidebar should match snapshot (auto-generated)', () => {
  const view = shallow(
    <PageSidebar className={"''"} nav={<div>ReactNode</div>} isManagedSidebar={true} isNavOpen={true} theme={'light'} />
  );
  expect(view).toMatchSnapshot();
});
