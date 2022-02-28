import * as React from 'react';
import { shallow } from 'enzyme';

import { ModalBox } from '../ModalBox';

test('ModalBox Test', () => {
  const view = shallow(
    <ModalBox title="Test Modal Box" id="boxId">
      This is a ModalBox
    </ModalBox>
  );
  expect(view).toMatchSnapshot();
});

test('ModalBox Test isLarge', () => {
  const view = shallow(
    <ModalBox title="Test Modal Box" id="boxId" isLarge>
      This is a ModalBox
    </ModalBox>
  );
  expect(view).toMatchSnapshot();
});

test('ModalBox Test isSmall', () => {
  const view = shallow(
    <ModalBox title="Test Modal Box" id="boxId" isSmall>
      This is a ModalBox
    </ModalBox>
  );
  expect(view).toMatchSnapshot();
});
