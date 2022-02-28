import * as React from 'react';
import * as ReactDOM from 'react-dom';
import { shallow } from 'enzyme';
import { AboutModal } from '../AboutModal';
import { KEY_CODES } from '../../../helpers/constants';

const mockListener = jest.spyOn(ReactDOM, 'createPortal');
jest.spyOn(document, 'createElement');
jest.spyOn(document.body, 'addEventListener');

mockListener.mockImplementation(node => node as React.ReactPortal);

const props = {
  onClose: jest.fn(),
  children: 'modal content',
  productName: 'Product Name',
  trademark: 'Trademark and copyright information here',
  brandImageSrc: 'brandImg...',
  brandImageAlt: 'Brand Image',
  logoImageSrc: 'logoImg...',
  logoImageAlt: 'AboutModal Logo'
};

test('AboutModal creates a container element once for div', () => {
  const view = shallow(<AboutModal {...props}> Test About Modal </AboutModal>);
  view.update();
  expect(document.createElement).toBeCalledWith('div');
  expect(document.createElement).toHaveBeenCalledTimes(1);
});

test('About Modal closes with escape', () => {
  shallow(
    <AboutModal {...props} isOpen>
      Test About Modal
    </AboutModal>
  );
  const [event, handler] = (document.body.addEventListener as any).mock.calls[0];
  expect(event).toBe('keydown');
  handler({ keyCode: KEY_CODES.ESCAPE_KEY });
  expect(props.onClose).toBeCalled();
});

test('modal does not call onClose for esc key if it is not open', () => {
  shallow(<AboutModal {...props} />);
  const [event, handler] = (document.body.addEventListener as any).mock.calls[0];
  expect(event).toBe('keydown');
  handler({ keyCode: KEY_CODES.ESCAPE_KEY });
  expect(props.onClose).not.toBeCalled();
});

test('Each modal is given new ariaDescribedById and ariaLabelledbyId', () => {
  const first = new AboutModal(props);
  const second = new AboutModal(props);
  expect(first.ariaLabelledBy).not.toBe(second.ariaLabelledBy);
  expect(first.ariaDescribedBy).not.toBe(second.ariaDescribedBy);
});

test('Console error is generated when the logoImageSrc is provided without logoImageAlt', () => {
  const noImgAltrops = {
    onClose: jest.fn(),
    children: 'modal content',
    productName: 'Product Name',
    trademark: 'Trademark and copyright information here',
    brandImageSrc: 'brandImg...',
    logoImageSrc: 'logoImg...'
  };
  const myMock = jest.fn() as any;
  global.console = { error: myMock } as any;
  const JSAboutModal = AboutModal as any;
  shallow(<JSAboutModal {...noImgAltrops}> Test About Modal </JSAboutModal>);
  expect(myMock).toBeCalled();
});
