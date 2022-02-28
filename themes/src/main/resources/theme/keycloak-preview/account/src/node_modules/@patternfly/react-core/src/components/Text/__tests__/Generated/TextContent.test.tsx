/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { TextContent } from '../../TextContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TextContent should match snapshot (auto-generated)', () => {
  const view = shallow(<TextContent children={<>ReactNode</>} className={"''"} />);
  expect(view).toMatchSnapshot();
});
