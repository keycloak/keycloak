/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Page } from '../../Page';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Page should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Page
      children={<>ReactNode</>}
      className={"''"}
      header={null}
      sidebar={null}
      skipToContent={null}
      role={'string'}
      mainContainerId={'null'}
      isManagedSidebar={false}
      defaultManagedSidebarIsOpen={true}
      onPageResize={(): void => null}
      breadcrumb={null}
      mainAriaLabel={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
