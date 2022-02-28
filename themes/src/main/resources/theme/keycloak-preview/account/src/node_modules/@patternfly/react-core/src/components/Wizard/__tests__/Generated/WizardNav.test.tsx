/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { WizardNav } from '../../WizardNav';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardNav should match snapshot (auto-generated)', () => {
  const view = shallow(<WizardNav children={'any'} ariaLabel={'string'} isOpen={false} returnList={false} />);
  expect(view).toMatchSnapshot();
});
