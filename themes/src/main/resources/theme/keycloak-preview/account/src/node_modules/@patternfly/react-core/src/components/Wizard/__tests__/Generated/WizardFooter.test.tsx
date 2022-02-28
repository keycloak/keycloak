/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { WizardFooter } from '../../WizardFooter';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardFooter should match snapshot (auto-generated)', () => {
  const view = shallow(<WizardFooter children={'any'} />);
  expect(view).toMatchSnapshot();
});
