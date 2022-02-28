/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { AccordionItem } from '../../AccordionItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AccordionItem should match snapshot (auto-generated)', () => {
  const view = shallow(<AccordionItem children={<>ReactNode</>} />);
  expect(view).toMatchSnapshot();
});
