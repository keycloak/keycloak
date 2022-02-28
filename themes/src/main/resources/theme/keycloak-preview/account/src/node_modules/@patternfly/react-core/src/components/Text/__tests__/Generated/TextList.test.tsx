/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { TextList } from '../../TextList';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TextList should match snapshot (auto-generated)', () => {
  const view = shallow(<TextList children={<>ReactNode</>} className={"''"} component={'ul'} />);
  expect(view).toMatchSnapshot();
});
