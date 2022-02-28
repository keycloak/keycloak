/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { CardHeader } from '../../CardHeader';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('CardHeader should match snapshot (auto-generated)', () => {
  const view = shallow(<CardHeader children={<>ReactNode</>} className={"''"} />);
  expect(view).toMatchSnapshot();
});
