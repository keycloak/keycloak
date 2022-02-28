/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { BreadcrumbHeading } from '../../BreadcrumbHeading';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('BreadcrumbHeading should match snapshot (auto-generated)', () => {
  const view = shallow(
    <BreadcrumbHeading children={<>ReactNode</>} className={"''"} to={'null'} target={'null'} component={'a'} />
  );
  expect(view).toMatchSnapshot();
});
