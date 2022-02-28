/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { Wizard } from '../../Wizard';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('Wizard should match snapshot (auto-generated)', () => {
  const view = shallow(
    <Wizard
      isOpen={false}
      isInPage={false}
      isCompactNav={false}
      isFullHeight={false}
      isFullWidth={false}
      width={null}
      height={null}
      title={"''"}
      description={"''"}
      onClose={() => undefined as any}
      onGoToStep={null}
      className={"''"}
      steps={[]}
      startAtStep={1}
      ariaLabelNav={"'Steps'"}
      hasBodyPadding={true}
      footer={null}
      onSave={() => undefined as void}
      onNext={null}
      onBack={null}
      nextButtonText={"'Next'"}
      backButtonText={"'Back'"}
      cancelButtonText={"'Cancel'"}
      ariaLabelCloseButton={"'Close'"}
      appendTo={null}
    />
  );
  expect(view).toMatchSnapshot();
});
