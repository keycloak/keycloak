/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ProgressBar } from '../../ProgressBar';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ProgressBar should match snapshot (auto-generated)', () => {
  const view = shallow(<ProgressBar children={<>ReactNode</>} className={"''"} value={42} ariaProps={undefined} />);
  expect(view).toMatchSnapshot();
});
