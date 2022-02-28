[![Build Status](https://secure.travis-ci.org/tdegrunt/jsonschema.svg)](http://travis-ci.org/tdegrunt/jsonschema)

# jsonschema
[JSON schema](http://json-schema.org/) validator, which is designed to be fast and simple to use.
The latest IETF published draft is v6, this library is mostly v4 compatible.

## Contributing & bugs
Please fork the repository, make the changes in your fork and include tests. Once you're done making changes, send in a pull request.

### Bug reports
Please include a test which shows why the code fails.

## Usage

### Simple
Simple object validation using JSON schemas.

```javascript
var Validator = require('jsonschema').Validator;
var v = new Validator();
var instance = 4;
var schema = {"type": "number"};
console.log(v.validate(instance, schema));
```

### Even simpler

```javascript
var validate = require('jsonschema').validate;
console.log(validate(4, {"type": "number"}));
```

### Complex example, with split schemas and references

```javascript
var Validator = require('jsonschema').Validator;
var v = new Validator();

// Address, to be embedded on Person
var addressSchema = {
  "id": "/SimpleAddress",
  "type": "object",
  "properties": {
    "lines": {
      "type": "array",
      "items": {"type": "string"}
    },
    "zip": {"type": "string"},
    "city": {"type": "string"},
    "country": {"type": "string"}
  },
  "required": ["country"]
};

// Person
var schema = {
  "id": "/SimplePerson",
  "type": "object",
  "properties": {
    "name": {"type": "string"},
    "address": {"$ref": "/SimpleAddress"},
    "votes": {"type": "integer", "minimum": 1}
  }
};

var p = {
  "name": "Barack Obama",
  "address": {
    "lines": [ "1600 Pennsylvania Avenue Northwest" ],
    "zip": "DC 20500",
    "city": "Washington",
    "country": "USA"
  },
  "votes": "lots"
};

v.addSchema(addressSchema, '/SimpleAddress');
console.log(v.validate(p, schema));
```
### Example for Array schema
```json
var arraySchema = {
        "type": "array",
        "items": {
            "properties": {
                "name": { "type": "string" },
                "lastname": { "type": "string" }
            },
            "required": ["name", "lastname"]
        }
    }
```
For a comprehensive, annotated example illustrating all possible validation options, see [examples/all.js](./examples/all.js)

## Features

### Definitions
All schema definitions are supported, $schema is ignored.

### Types
All types are supported

### Formats
#### Disabling the format keyword.

You may disable format validation by providing `disableFormat: true` to the validator
options.

#### String Formats
All formats are supported, phone numbers are expected to follow the [E.123](http://en.wikipedia.org/wiki/E.123) standard.

#### Custom Formats
You may add your own custom format functions.  Format functions accept the input
being validated and return a boolean value.  If the returned value is `true`, then
validation succeeds.  If the returned value is `false`, then validation fails.

* Formats added to `Validator.prototype.customFormats` do not affect previously instantiated
Validators.  This is to prevent validator instances from being altered once created.
It is conceivable that multiple validators may be created to handle multiple schemas
with different formats in a program.
* Formats added to `validator.customFormats` affect only that Validator instance.

Here is an example that uses custom formats:

```javascript
Validator.prototype.customFormats.myFormat = function(input) {
  return input === 'myFormat';
};

var validator = new Validator();
validator.validate('myFormat', {type: 'string', format: 'myFormat'}).valid; // true
validator.validate('foo', {type: 'string', format: 'myFormat'}).valid; // false
```

### Results
The first error found will be thrown as an `Error` object if `options.throwError` is `true`.  Otherwise all results will be appended to the `result.errors` array which also contains the success flag `result.valid`.

When `oneOf` or `anyOf` validations fail, errors that caused any of the sub-schemas referenced therein to fail are not reported, unless `options.nestedErrors` is truthy. This option may be useful when troubleshooting validation errors in complex schemas. 

### Custom properties
Specify your own JSON Schema properties with the validator.attributes property:

```javascript
validator.attributes.contains = function validateContains(instance, schema, options, ctx) {
  if(typeof instance!='string') return;
  if(typeof schema.contains!='string') throw new jsonschema.SchemaError('"contains" expects a string', schema);
  if(instance.indexOf(schema.contains)<0){
    return 'does not contain the string ' + JSON.stringify(schema.contains);
  }
}
var result = validator.validate("i am an instance", { type:"string", contains: "i am" });
// result.valid === true;
```

The instance passes validation if the function returns nothing. A single validation error is produced
if the fuction returns a string. Any number of errors (maybe none at all) may be returned by passing a
`ValidatorResult` object, which may be used like so:

```javascript
  var result = new ValidatorResult(instance, schema, options, ctx);
  while(someErrorCondition()){
    result.addError('fails some validation test');
  }
  return result;
```

### Dereferencing schemas
Sometimes you may want to download schemas from remote sources, like a database, or over HTTP. When importing a schema,
unknown references are inserted into the `validator.unresolvedRefs` Array. Asynchronously shift elements off this array and import
them:

```javascript
var Validator = require('jsonschema').Validator;
var v = new Validator();
v.addSchema(initialSchema);
function importNextSchema(){
  var nextSchema = v.unresolvedRefs.shift();
  if(!nextSchema){ done(); return; }
  databaseGet(nextSchema, function(schema){
    v.addSchema(schema);
    importNextSchema();
  });
}
importNextSchema();
```

### Pre-Property Validation Hook
If some processing of properties is required prior to validation a function may be passed via the options parameter of the validate function. For example, say you needed to perform type coercion for some properties:

```javascript
const coercionHook = function (instance, property, schema, options, ctx) {
  var value = instance[property];

  // Skip nulls and undefineds
  if (value === null || typeof value == 'undefined') {
    return;
  }

  // If the schema declares a type and the property fails type validation.
  if (schema.type && this.attributes.type.call(this, instance, schema, options, ctx.makeChild(schema, property))) {
    var types = Array.isArray(schema.type) ? schema.type : [schema.type];
    var coerced = undefined;

    // Go through the declared types until we find something that we can
    // coerce the value into.
    for (var i = 0; typeof coerced == 'undefined' && i < types.length; i++) {
      // If we support coercion to this type
      if (lib.coercions[types[i]]) {
        // ...attempt it.
        coerced = lib.coercions[types[i]](value);
      }
    }
    // If we got a successful coercion we modify the property of the instance.
    if (typeof coerced != 'undefined') {
      instance[property] = coerced;
    }
  }
}.bind(validator)

// And now, to actually perform validation with the coercion hook!
v.validate(instance, schema, { preValidateProperty: coercionHook });
```

## Tests
Uses [JSON Schema Test Suite](https://github.com/json-schema/JSON-Schema-Test-Suite) as well as our own tests.
You'll need to update and init the git submodules:

    git submodule update --init
    npm test

## Contributions

This library would not be possible without the valuable contributions by:

- Austin Wright

... and many others!

## License

    jsonschema is licensed under MIT license.

    Copyright (C) 2012-2019 Tom de Grunt <tom@degrunt.nl>

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
    of the Software, and to permit persons to whom the Software is furnished to do
    so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
