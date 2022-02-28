/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { WizardFooterInternal } from '../../WizardFooterInternal';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardFooterInternal should match snapshot (auto-generated)', () => {
  const view = shallow(
    <WizardFooterInternal
      onNext={'any'}
      onBack={'any'}
      onClose={'any'}
      isValid={true}
      firstStep={true}
      activeStep={{ name: 'step' }}
      nextButtonText={'string'}
      backButtonText={'string'}
      cancelButtonText={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
