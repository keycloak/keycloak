/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { AlertIcon } from '../../AlertIcon';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AlertIcon should match snapshot (auto-generated)', () => {
  const view = shallow(<AlertIcon variant={'success'} className={"''"} />);
  expect(view).toMatchSnapshot();
});
