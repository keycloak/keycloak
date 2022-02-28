/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { FlexItem } from '../../FlexItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('FlexItem should match snapshot (auto-generated)', () => {
  const view = shallow(<FlexItem children={<>ReactNode</>} className={"''"} breakpointMods={[]} />);
  expect(view).toMatchSnapshot();
});
