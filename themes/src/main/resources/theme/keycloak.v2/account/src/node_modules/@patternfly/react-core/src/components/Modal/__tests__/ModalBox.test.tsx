import * as React from 'react';
import { render } from '@testing-library/react';

import { ModalBox } from '../ModalBox';

test('ModalBox Test', () => {
  const { asFragment } = render(
    <ModalBox aria-describedby="Test Modal Box" id="boxId">
      This is a ModalBox
    </ModalBox>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('ModalBox Test large', () => {
  const { asFragment } = render(
    <ModalBox aria-describedby="Test Modal Box" id="boxId" variant="large">
      This is a ModalBox
    </ModalBox>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('ModalBox Test medium', () => {
  const { asFragment } = render(
    <ModalBox aria-describedby="Test Modal Box" id="boxId" variant="medium">
      This is a ModalBox
    </ModalBox>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('ModalBox Test small', () => {
  const { asFragment } = render(
    <ModalBox aria-describedby="Test Modal Box" id="boxId" variant="small">
      This is a ModalBox
    </ModalBox>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('ModalBox Test top aligned', () => {
  const { asFragment } = render(
    <ModalBox aria-describedby="Test Modal Box" id="boxId" position="top">
      This is a ModalBox
    </ModalBox>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('ModalBox Test top aligned distance', () => {
  const { asFragment } = render(
    <ModalBox aria-describedby="Test Modal Box" id="boxId" position="top" positionOffset="50px">
      This is a ModalBox
    </ModalBox>
  );
  expect(asFragment()).toMatchSnapshot();
});
