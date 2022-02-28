/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalContainer } from '../../AboutModalContainer';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AboutModalContainer should match snapshot (auto-generated)', () => {
  const view = shallow(
    <AboutModalContainer
      children={<div>ReactNode</div>}
      className={"''"}
      isOpen={false}
      onClose={() => undefined}
      productName={"''"}
      trademark={'string'}
      brandImageSrc={'string'}
      brandImageAlt={'string'}
      backgroundImageSrc={'string'}
      ariaLabelledbyId={'string'}
      ariaDescribedById={'string'}
    />
  );
  expect(view).toMatchSnapshot();
});
