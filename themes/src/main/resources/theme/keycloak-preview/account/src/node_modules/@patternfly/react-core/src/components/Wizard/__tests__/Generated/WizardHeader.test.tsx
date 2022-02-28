/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { WizardHeader } from '../../WizardHeader';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardHeader should match snapshot (auto-generated)', () => {
  const view = shallow(
    <WizardHeader
      onClose={() => undefined}
      title={'string'}
      description={'string'}
      ariaLabelCloseButton={'string'}
      titleId={'string'}
      descriptionId={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
