/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LoginPage } from '../../LoginPage';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginPage should match snapshot (auto-generated)', () => {
  const view = shallow(
    <LoginPage
      children={<>ReactNode</>}
      className={"''"}
      brandImgSrc={"''"}
      brandImgAlt={"''"}
      backgroundImgSrc={''}
      backgroundImgAlt={"''"}
      textContent={"''"}
      footerListItems={null}
      footerListVariants={undefined}
      loginTitle={'string'}
      loginSubtitle={'string'}
      signUpForAccountMessage={null}
      forgotCredentials={null}
      socialMediaLoginContent={null}
    />
  );
  expect(view).toMatchSnapshot();
});
