/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { InputGroupText } from '../../InputGroupText';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('InputGroupText should match snapshot (auto-generated)', () => {
  const view = shallow(<InputGroupText className={"''"} children={<div>ReactNode</div>} component={'span'} />);
  expect(view).toMatchSnapshot();
});
