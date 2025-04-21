import expect from 'expect';
import { generateObjSchema, arraySchema, enumArraySchema } from '../../../src/util/schemas';

describe('schemas', () => {
  it('should generate an object schema with correct properties', () => {
    const schema = generateObjSchema({
      foo: 'bar',
      baz: arraySchema,
    });
    const properties = schema.properties || {};

    expect(properties.foo).toEqual(properties.foo, 'bar');
    expect(properties.baz.type).toEqual('array');
  });
  describe('enumArraySchema', () => {
    it('works with no arguments', () => {
      expect(enumArraySchema()).toEqual({
        additionalItems: false,
        items: {
          enum: [],
          type: 'string',
        },
        minItems: 0,
        type: 'array',
        uniqueItems: true,
      });
    });
  });
});
