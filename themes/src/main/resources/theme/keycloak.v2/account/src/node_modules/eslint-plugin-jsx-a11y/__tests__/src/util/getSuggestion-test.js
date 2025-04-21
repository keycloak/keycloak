import expect from 'expect';
import getSuggestion from '../../../src/util/getSuggestion';

describe('spell check suggestion API', () => {
  it('should return no suggestions given empty word and no dictionary', () => {
    const word = '';
    const expected = [];
    const actual = getSuggestion(word);

    expect(expected).toEqual(actual);
  });

  it('should return no suggestions given real word and no dictionary', () => {
    const word = 'foo';
    const expected = [];
    const actual = getSuggestion(word);

    expect(expected).toEqual(actual);
  });

  it('should return correct suggestion given real word and a dictionary', () => {
    const word = 'fo';
    const dictionary = ['foo', 'bar', 'baz'];
    const expected = ['foo'];
    const actual = getSuggestion(word, dictionary);

    expect(expected).toEqual(actual);
  });

  it('should return multiple correct suggestions given real word and a dictionary', () => {
    const word = 'theer';
    const dictionary = ['there', 'their', 'foo', 'bar'];
    const expected = ['there', 'their'];
    const actual = getSuggestion(word, dictionary);

    expect(expected).toEqual(actual);
  });

  it('should return correct # of suggestions given the limit argument', () => {
    const word = 'theer';
    const dictionary = ['there', 'their', 'foo', 'bar'];
    const limit = 1;
    const expected = 1;
    const actual = getSuggestion(word, dictionary, limit).length;

    expect(expected).toEqual(actual);
  });
});
