/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Toolbar } from '../../Toolbar';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Toolbar should match snapshot (auto-generated)', () => {
  const view = shallow(<Toolbar children={<>ReactNode</>} className={'null'} />);
  expect(view).toMatchSnapshot();
});
