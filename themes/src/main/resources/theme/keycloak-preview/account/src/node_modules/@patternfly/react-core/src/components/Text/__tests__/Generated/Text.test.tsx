/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Text } from '../../Text';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Text should match snapshot (auto-generated)', () => {
  const view = shallow(<Text component={'h1'} children={<>ReactNode</>} className={"''"} />);
  expect(view).toMatchSnapshot();
});
