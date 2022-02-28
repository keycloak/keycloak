/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { WizardBody } from '../../WizardBody';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardBody should match snapshot (auto-generated)', () => {
  const view = shallow(<WizardBody children={'any'} hasBodyPadding={true} />);
  expect(view).toMatchSnapshot();
});
