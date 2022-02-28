/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { CardBody } from '../../CardBody';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('CardBody should match snapshot (auto-generated)', () => {
  const view = shallow(<CardBody children={<>ReactNode</>} className={"''"} component={'div'} isFilled={true} />);
  expect(view).toMatchSnapshot();
});
