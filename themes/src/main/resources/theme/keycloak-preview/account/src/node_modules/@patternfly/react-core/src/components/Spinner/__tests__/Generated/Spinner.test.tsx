/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Spinner } from '../../Spinner';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Spinner should match snapshot (auto-generated)', () => {
  const view = shallow(<Spinner className={"''"} size={'xl'} aria-valuetext={"'Loading...'"} />);
  expect(view).toMatchSnapshot();
});
