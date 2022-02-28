import * as React from 'react';
import { shallow } from 'enzyme';

import { ModalBoxBody } from '../ModalBoxBody';

test('ModalBoxBody Test', () => {
  const view = shallow(
    <ModalBoxBody id="id" className="test-box-class">
      This is a ModalBox header
    </ModalBoxBody>
  );
  expect(view).toMatchSnapshot();
});
