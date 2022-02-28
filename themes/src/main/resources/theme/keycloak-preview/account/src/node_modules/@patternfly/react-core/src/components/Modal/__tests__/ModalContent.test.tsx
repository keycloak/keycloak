import * as React from 'react';
import { shallow } from 'enzyme';

import { ModalContent } from '../ModalContent';

test('Modal Content Test only body', () => {
  const view = shallow(
    <ModalContent title="Test Modal Content title" id="id" isOpen>
      This is a ModalBox header
    </ModalContent>
  );
  expect(view).toMatchSnapshot();
});

test('Modal Content Test isOpen', () => {
  const view = shallow(
    <ModalContent title="Test Modal Content title" id="id" isOpen>
      This is a ModalBox header
    </ModalContent>
  );
  expect(view).toMatchSnapshot();
});

test('Modal Content Test description', () => {
  const view = shallow(
    <ModalContent title="Test Modal Content title" id="id" isOpen description="This is a test description.">
      This is a ModalBox header
    </ModalContent>
  );
  expect(view).toMatchSnapshot();
});

test('Modal Content Test with footer', () => {
  const view = shallow(
    <ModalContent title="Test Modal Content title" id="id" isOpen actions={['Testing']}>
      This is a ModalBox header
    </ModalContent>
  );
  expect(view).toMatchSnapshot();
});

test('Modal Content test without footer', () => {
  const view = shallow(
    <ModalContent title="Test Modal Content title" id="id" isOpen>
      This is a ModalBox header
    </ModalContent>
  );
  expect(view).toMatchSnapshot();
});

test('Modal Content Test with onclose', () => {
  const view = shallow(
    <ModalContent
      title="Test Modal Content title"
      actions={['Testing footer']}
      isLarge
      onClose={() => undefined}
      id="id"
      isOpen
    >
      This is a ModalBox header
    </ModalContent>
  );
  expect(view).toMatchSnapshot();
});

test('Modal Test with custom header', () => {
  const header = <span id="test-custom-header">TEST</span>;

  const view = shallow(
    <ModalContent
      header={header}
      title="test-custom-header-modal"
      actions={['Testing footer']}
      isLarge
      onClose={() => undefined}
      id="id"
      isOpen
    >
      This is a ModalBox header
    </ModalContent>
  );
  expect(view).toMatchSnapshot();
});

test('Modal Test with custom footer', () => {
  const footer = <span id="test-custom-footer">TEST</span>;

  const view = shallow(
    <ModalContent footer={footer} title="Test Modal Custom Footer" isLarge onClose={() => undefined} id="id" isOpen>
      This is a ModalBox header
    </ModalContent>
  );
  expect(view).toMatchSnapshot();
});
