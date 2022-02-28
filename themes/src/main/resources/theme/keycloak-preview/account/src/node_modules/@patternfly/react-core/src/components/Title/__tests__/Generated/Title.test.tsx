/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Title } from '../../Title';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Title should match snapshot (auto-generated)', () => {
  const view = shallow(<Title size={'xs'} children={''} className={"''"} headingLevel={'h1'} />);
  expect(view).toMatchSnapshot();
});
