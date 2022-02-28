/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { BreadcrumbItem } from '../../BreadcrumbItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('BreadcrumbItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <BreadcrumbItem
      children={<>ReactNode</>}
      className={"''"}
      to={'null'}
      isActive={false}
      target={'null'}
      component={'a'}
    />
  );
  expect(view).toMatchSnapshot();
});
