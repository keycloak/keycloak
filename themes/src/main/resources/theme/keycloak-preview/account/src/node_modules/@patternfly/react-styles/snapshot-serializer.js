const { default: serializer, createSerializer } = require('./dist/js/build/jest/snapshot-serializer');

module.exports = serializer;
module.exports.createSerializer = createSerializer;
