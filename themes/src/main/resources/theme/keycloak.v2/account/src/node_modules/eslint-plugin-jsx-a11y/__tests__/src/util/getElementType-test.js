import expect from 'expect';
import getElementType from '../../../src/util/getElementType';
import JSXElementMock from '../../../__mocks__/JSXElementMock';

describe('getElementType', () => {
  describe('no settings in context', () => {
    const elementType = getElementType({ settings: {} });

    it('should return the exact tag name for a DOM element', () => {
      expect(elementType(JSXElementMock('input').openingElement)).toBe('input');
    });

    it('should return the exact tag name for a custom element', () => {
      expect(elementType(JSXElementMock('CustomInput').openingElement)).toBe('CustomInput');
    });

    it('should return the exact tag name for names that are in Object.prototype', () => {
      expect(elementType(JSXElementMock('toString').openingElement)).toBe('toString');
    });
  });

  describe('components settings in context', () => {
    const elementType = getElementType({
      settings: {
        'jsx-a11y': {
          components: {
            CustomInput: 'input',
          },
        },
      },
    });

    it('should return the exact tag name for a DOM element', () => {
      expect(elementType(JSXElementMock('input').openingElement)).toBe('input');
    });

    it('should return the mapped tag name for a custom element', () => {
      expect(elementType(JSXElementMock('CustomInput').openingElement)).toBe('input');
    });

    it('should return the exact tag name for a custom element not in the components map', () => {
      expect(elementType(JSXElementMock('CityInput').openingElement)).toBe('CityInput');
    });
  });
});
