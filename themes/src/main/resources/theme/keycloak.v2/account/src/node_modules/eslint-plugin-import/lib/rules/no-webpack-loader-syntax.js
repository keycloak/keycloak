'use strict';var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

function reportIfNonStandard(context, node, name) {
  if (name && name.indexOf('!') !== -1) {
    context.report(node, 'Unexpected \'!\' in \'' + String(name) + '\'. ' +
    'Do not use import syntax to configure webpack loaders.');

  }
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2['default'])('no-webpack-loader-syntax') },

    schema: [] },


  create: function () {function create(context) {
      return (0, _moduleVisitor2['default'])(function (source, node) {
        reportIfNonStandard(context, node, source.value);
      }, { commonjs: true });
    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby13ZWJwYWNrLWxvYWRlci1zeW50YXguanMiXSwibmFtZXMiOlsicmVwb3J0SWZOb25TdGFuZGFyZCIsImNvbnRleHQiLCJub2RlIiwibmFtZSIsImluZGV4T2YiLCJyZXBvcnQiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwic291cmNlIiwidmFsdWUiLCJjb21tb25qcyJdLCJtYXBwaW5ncyI6ImFBQUEsa0U7QUFDQSxxQzs7QUFFQSxTQUFTQSxtQkFBVCxDQUE2QkMsT0FBN0IsRUFBc0NDLElBQXRDLEVBQTRDQyxJQUE1QyxFQUFrRDtBQUNoRCxNQUFJQSxRQUFRQSxLQUFLQyxPQUFMLENBQWEsR0FBYixNQUFzQixDQUFDLENBQW5DLEVBQXNDO0FBQ3BDSCxZQUFRSSxNQUFSLENBQWVILElBQWYsRUFBcUIsa0NBQXNCQyxJQUF0QjtBQUNuQiw0REFERjs7QUFHRDtBQUNGOztBQUVERyxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxTQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSywwQkFBUSwwQkFBUixDQURELEVBRkY7O0FBS0pDLFlBQVEsRUFMSixFQURTOzs7QUFTZkMsUUFUZSwrQkFTUlosT0FUUSxFQVNDO0FBQ2QsYUFBTyxnQ0FBYyxVQUFDYSxNQUFELEVBQVNaLElBQVQsRUFBa0I7QUFDckNGLDRCQUFvQkMsT0FBcEIsRUFBNkJDLElBQTdCLEVBQW1DWSxPQUFPQyxLQUExQztBQUNELE9BRk0sRUFFSixFQUFFQyxVQUFVLElBQVosRUFGSSxDQUFQO0FBR0QsS0FiYyxtQkFBakIiLCJmaWxlIjoibm8td2VicGFjay1sb2FkZXItc3ludGF4LmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IG1vZHVsZVZpc2l0b3IgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5mdW5jdGlvbiByZXBvcnRJZk5vblN0YW5kYXJkKGNvbnRleHQsIG5vZGUsIG5hbWUpIHtcbiAgaWYgKG5hbWUgJiYgbmFtZS5pbmRleE9mKCchJykgIT09IC0xKSB7XG4gICAgY29udGV4dC5yZXBvcnQobm9kZSwgYFVuZXhwZWN0ZWQgJyEnIGluICcke25hbWV9Jy4gYCArXG4gICAgICAnRG8gbm90IHVzZSBpbXBvcnQgc3ludGF4IHRvIGNvbmZpZ3VyZSB3ZWJwYWNrIGxvYWRlcnMuJyxcbiAgICApO1xuICB9XG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3Byb2JsZW0nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8td2VicGFjay1sb2FkZXItc3ludGF4JyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuXG4gIGNyZWF0ZShjb250ZXh0KSB7XG4gICAgcmV0dXJuIG1vZHVsZVZpc2l0b3IoKHNvdXJjZSwgbm9kZSkgPT4ge1xuICAgICAgcmVwb3J0SWZOb25TdGFuZGFyZChjb250ZXh0LCBub2RlLCBzb3VyY2UudmFsdWUpO1xuICAgIH0sIHsgY29tbW9uanM6IHRydWUgfSk7XG4gIH0sXG59O1xuIl19