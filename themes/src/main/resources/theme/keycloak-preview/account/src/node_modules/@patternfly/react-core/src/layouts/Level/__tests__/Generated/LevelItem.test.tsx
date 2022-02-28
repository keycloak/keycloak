/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LevelItem } from '../../LevelItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LevelItem should match snapshot (auto-generated)', () => {
  const view = shallow(<LevelItem children={<>ReactNode</>} />);
  expect(view).toMatchSnapshot();
});
