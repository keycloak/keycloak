/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { CardHead } from '../../CardHead';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('CardHead should match snapshot (auto-generated)', () => {
  const view = shallow(<CardHead children={<>ReactNode</>} className={"''"} />);
  expect(view).toMatchSnapshot();
});
