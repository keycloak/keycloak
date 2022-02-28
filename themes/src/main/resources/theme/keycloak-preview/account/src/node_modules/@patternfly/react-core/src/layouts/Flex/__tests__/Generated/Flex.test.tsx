/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Flex } from '../../Flex';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Flex should match snapshot (auto-generated)', () => {
  const view = shallow(<Flex children={<>ReactNode</>} className={"''"} breakpointMods={[]} />);
  expect(view).toMatchSnapshot();
});
