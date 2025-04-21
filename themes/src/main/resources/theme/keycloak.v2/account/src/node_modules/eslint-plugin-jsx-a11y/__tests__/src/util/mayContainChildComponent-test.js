import expect from 'expect';
import mayContainChildComponent from '../../../src/util/mayContainChildComponent';
import JSXAttributeMock from '../../../__mocks__/JSXAttributeMock';
import JSXElementMock from '../../../__mocks__/JSXElementMock';
import JSXExpressionContainerMock from '../../../__mocks__/JSXExpressionContainerMock';

describe('mayContainChildComponent', () => {
  describe('no FancyComponent', () => {
    it('should return false', () => {
      expect(mayContainChildComponent(
        JSXElementMock('div', [], [
          JSXElementMock('div', [], [
            JSXElementMock('span', [], []),
            JSXElementMock('span', [], [
              JSXElementMock('span', [], []),
              JSXElementMock('span', [], [
                JSXElementMock('span', [], []),
              ]),
            ]),
          ]),
          JSXElementMock('span', [], []),
          JSXElementMock('img', [
            JSXAttributeMock('src', 'some/path'),
          ]),
        ]),
        'FancyComponent',
        5,
      )).toBe(false);
    });
  });
  describe('contains an indicated component', () => {
    it('should return true', () => {
      expect(mayContainChildComponent(
        JSXElementMock('div', [], [
          JSXElementMock('input'),
        ]),
        'input',
      )).toBe(true);
    });
    it('should return true', () => {
      expect(mayContainChildComponent(
        JSXElementMock('div', [], [
          JSXElementMock('FancyComponent'),
        ]),
        'FancyComponent',
      )).toBe(true);
    });
    it('FancyComponent is outside of default depth, should return false', () => {
      expect(mayContainChildComponent(
        JSXElementMock('div', [], [
          JSXElementMock('div', [], [
            JSXElementMock('FancyComponent'),
          ]),
        ]),
        'FancyComponent',
      )).toBe(false);
    });
    it('FancyComponent is inside of custom depth, should return true', () => {
      expect(mayContainChildComponent(
        JSXElementMock('div', [], [
          JSXElementMock('div', [], [
            JSXElementMock('FancyComponent'),
          ]),
        ]),
        'FancyComponent',
        2,
      )).toBe(true);
    });
    it('deep nesting, should return true', () => {
      expect(mayContainChildComponent(
        JSXElementMock('div', [], [
          JSXElementMock('div', [], [
            JSXElementMock('span', [], []),
            JSXElementMock('span', [], [
              JSXElementMock('span', [], []),
              JSXElementMock('span', [], [
                JSXElementMock('span', [], [
                  JSXElementMock('span', [], [
                    JSXElementMock('FancyComponent'),
                  ]),
                ]),
              ]),
            ]),
          ]),
          JSXElementMock('span', [], []),
          JSXElementMock('img', [
            JSXAttributeMock('src', 'some/path'),
          ]),
        ]),
        'FancyComponent',
        6,
      )).toBe(true);
    });
  });
  describe('Intederminate situations', () => {
    describe('expression container children', () => {
      it('should return true', () => {
        expect(mayContainChildComponent(
          JSXElementMock('div', [], [
            JSXExpressionContainerMock('mysteryBox'),
          ]),
          'FancyComponent',
        )).toBe(true);
      });
    });
  });

  describe('Glob name matching', () => {
    describe('component name contains question mark ? - match any single character', () => {
      it('should return true', () => {
        expect(mayContainChildComponent(
          JSXElementMock('div', [], [
            JSXElementMock('FancyComponent'),
          ]),
          'Fanc?Co??onent',
        )).toBe(true);
      });
      it('should return false', () => {
        expect(mayContainChildComponent(
          JSXElementMock('div', [], [
            JSXElementMock('FancyComponent'),
          ]),
          'FancyComponent?',
        )).toBe(false);
      });
    });

    describe('component name contains asterisk * - match zero or more characters', () => {
      it('should return true', () => {
        expect(mayContainChildComponent(
          JSXElementMock('div', [], [
            JSXElementMock('FancyComponent'),
          ]),
          'Fancy*',
        )).toBe(true);
      });
      it('should return true', () => {
        expect(mayContainChildComponent(
          JSXElementMock('div', [], [
            JSXElementMock('FancyComponent'),
          ]),
          '*Component',
        )).toBe(true);
      });
      it('should return true', () => {
        expect(mayContainChildComponent(
          JSXElementMock('div', [], [
            JSXElementMock('FancyComponent'),
          ]),
          'Fancy*C*t',
        )).toBe(true);
      });
    });
  });

  describe('using a custom elementType function', () => {
    it('should return true when the custom elementType returns the proper name', () => {
      expect(mayContainChildComponent(
        JSXElementMock('div', [], [
          JSXElementMock('CustomInput'),
        ]),
        'input',
        2,
        () => 'input',
      )).toBe(true);
    });
    it('should return false when the custom elementType returns a wrong name', () => {
      expect(mayContainChildComponent(
        JSXElementMock('div', [], [
          JSXElementMock('CustomInput'),
        ]),
        'input',
        2,
        () => 'button',
      )).toBe(false);
    });
  });
});
