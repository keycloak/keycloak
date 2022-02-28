/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { InputGroup } from '../../InputGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('InputGroup should match snapshot (auto-generated)', () => {
  const view = shallow(<InputGroup className={"''"} children={<div>ReactNode</div>} />);
  expect(view).toMatchSnapshot();
});
