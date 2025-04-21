/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { WizardHeader } from '../../WizardHeader';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('WizardHeader should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <WizardHeader
      onClose={() => undefined}
      title={'string'}
      description={<div>ReactNode</div>}
      hideClose={true}
      closeButtonAriaLabel={'string'}
      titleId={'string'}
      descriptionId={'string'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
