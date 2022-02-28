/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalBoxHeader } from '../../AboutModalBoxHeader';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AboutModalBoxHeader should match snapshot (auto-generated)', () => {
  const view = shallow(<AboutModalBoxHeader className={"''"} productName={"''"} id={'string'} />);
  expect(view).toMatchSnapshot();
});
