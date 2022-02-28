/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalBox } from '../../AboutModalBox';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AboutModalBox should match snapshot (auto-generated)', () => {
  const view = shallow(<AboutModalBox children={<div>ReactNode</div>} className={"''"} />);
  expect(view).toMatchSnapshot();
});
