/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { CardFooter } from '../../CardFooter';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('CardFooter should match snapshot (auto-generated)', () => {
  const view = shallow(<CardFooter children={<>ReactNode</>} className={"''"} component={'div'} />);
  expect(view).toMatchSnapshot();
});
