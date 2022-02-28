/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { SimpleListGroup } from '../../SimpleListGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('SimpleListGroup should match snapshot (auto-generated)', () => {
  const view = shallow(
    <SimpleListGroup children={<>ReactNode</>} className={"''"} titleClassName={"''"} title={''} id={"''"} />
  );
  expect(view).toMatchSnapshot();
});
