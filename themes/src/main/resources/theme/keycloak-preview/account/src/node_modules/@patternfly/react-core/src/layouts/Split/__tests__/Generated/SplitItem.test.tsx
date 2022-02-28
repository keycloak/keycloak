/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { SplitItem } from '../../SplitItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('SplitItem should match snapshot (auto-generated)', () => {
  const view = shallow(<SplitItem isFilled={false} children={<>ReactNode</>} className={"''"} />);
  expect(view).toMatchSnapshot();
});
