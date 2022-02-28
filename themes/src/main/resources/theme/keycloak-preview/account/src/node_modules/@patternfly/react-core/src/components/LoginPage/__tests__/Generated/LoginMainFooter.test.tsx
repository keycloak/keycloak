/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { LoginMainFooter } from '../../LoginMainFooter';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('LoginMainFooter should match snapshot (auto-generated)', () => {
  const view = shallow(
    <LoginMainFooter
      className={"''"}
      children={<>ReactNode</>}
      socialMediaLoginContent={null}
      signUpForAccountMessage={null}
      forgotCredentials={null}
    />
  );
  expect(view).toMatchSnapshot();
});
