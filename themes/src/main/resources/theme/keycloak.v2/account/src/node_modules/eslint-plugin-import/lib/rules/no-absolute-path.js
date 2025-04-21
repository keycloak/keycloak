'use strict';var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _importType = require('../core/importType');
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-absolute-path') },

    schema: [(0, _moduleVisitor.makeOptionsSchema)()] },


  create: function () {function create(context) {
      function reportIfAbsolute(source) {
        if ((0, _importType.isAbsolute)(source.value)) {
          context.report(source, 'Do not import modules using an absolute path');
        }
      }

      var options = Object.assign({ esmodule: true, commonjs: true }, context.options[0]);
      return (0, _moduleVisitor2['default'])(reportIfAbsolute, options);
    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1hYnNvbHV0ZS1wYXRoLmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJjcmVhdGUiLCJjb250ZXh0IiwicmVwb3J0SWZBYnNvbHV0ZSIsInNvdXJjZSIsInZhbHVlIiwicmVwb3J0Iiwib3B0aW9ucyIsIk9iamVjdCIsImFzc2lnbiIsImVzbW9kdWxlIiwiY29tbW9uanMiXSwibWFwcGluZ3MiOiJhQUFBLGtFO0FBQ0E7QUFDQSxxQzs7QUFFQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssMEJBQVEsa0JBQVIsQ0FERCxFQUZGOztBQUtKQyxZQUFRLENBQUUsdUNBQUYsQ0FMSixFQURTOzs7QUFTZkMsUUFUZSwrQkFTUkMsT0FUUSxFQVNDO0FBQ2QsZUFBU0MsZ0JBQVQsQ0FBMEJDLE1BQTFCLEVBQWtDO0FBQ2hDLFlBQUksNEJBQVdBLE9BQU9DLEtBQWxCLENBQUosRUFBOEI7QUFDNUJILGtCQUFRSSxNQUFSLENBQWVGLE1BQWYsRUFBdUIsOENBQXZCO0FBQ0Q7QUFDRjs7QUFFRCxVQUFNRyxVQUFVQyxPQUFPQyxNQUFQLENBQWMsRUFBRUMsVUFBVSxJQUFaLEVBQWtCQyxVQUFVLElBQTVCLEVBQWQsRUFBa0RULFFBQVFLLE9BQVIsQ0FBZ0IsQ0FBaEIsQ0FBbEQsQ0FBaEI7QUFDQSxhQUFPLGdDQUFjSixnQkFBZCxFQUFnQ0ksT0FBaEMsQ0FBUDtBQUNELEtBbEJjLG1CQUFqQiIsImZpbGUiOiJuby1hYnNvbHV0ZS1wYXRoLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IG1vZHVsZVZpc2l0b3IsIHsgbWFrZU9wdGlvbnNTY2hlbWEgfSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL21vZHVsZVZpc2l0b3InO1xuaW1wb3J0IHsgaXNBYnNvbHV0ZSB9IGZyb20gJy4uL2NvcmUvaW1wb3J0VHlwZSc7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1hYnNvbHV0ZS1wYXRoJyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFsgbWFrZU9wdGlvbnNTY2hlbWEoKSBdLFxuICB9LFxuXG4gIGNyZWF0ZShjb250ZXh0KSB7XG4gICAgZnVuY3Rpb24gcmVwb3J0SWZBYnNvbHV0ZShzb3VyY2UpIHtcbiAgICAgIGlmIChpc0Fic29sdXRlKHNvdXJjZS52YWx1ZSkpIHtcbiAgICAgICAgY29udGV4dC5yZXBvcnQoc291cmNlLCAnRG8gbm90IGltcG9ydCBtb2R1bGVzIHVzaW5nIGFuIGFic29sdXRlIHBhdGgnKTtcbiAgICAgIH1cbiAgICB9XG5cbiAgICBjb25zdCBvcHRpb25zID0gT2JqZWN0LmFzc2lnbih7IGVzbW9kdWxlOiB0cnVlLCBjb21tb25qczogdHJ1ZSB9LCBjb250ZXh0Lm9wdGlvbnNbMF0pO1xuICAgIHJldHVybiBtb2R1bGVWaXNpdG9yKHJlcG9ydElmQWJzb2x1dGUsIG9wdGlvbnMpO1xuICB9LFxufTtcbiJdfQ==