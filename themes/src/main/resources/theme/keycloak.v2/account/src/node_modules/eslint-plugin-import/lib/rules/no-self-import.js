'use strict';




var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);
var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

function isImportingSelf(context, node, requireName) {
  var filePath = context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename();

  // If the input is from stdin, this test can't fail
  if (filePath !== '<text>' && filePath === (0, _resolve2['default'])(requireName, context)) {
    context.report({
      node: node,
      message: 'Module imports itself.' });

  }
} /**
   * @fileOverview Forbids a module from importing itself
   * @author Gio d'Amelio
   */module.exports = { meta: {
    type: 'problem',
    docs: {
      description: 'Forbid a module from importing itself',
      recommended: true,
      url: (0, _docsUrl2['default'])('no-self-import') },


    schema: [] },

  create: function () {function create(context) {
      return (0, _moduleVisitor2['default'])(function (source, node) {
        isImportingSelf(context, node, source.value);
      }, { commonjs: true });
    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1zZWxmLWltcG9ydC5qcyJdLCJuYW1lcyI6WyJpc0ltcG9ydGluZ1NlbGYiLCJjb250ZXh0Iiwibm9kZSIsInJlcXVpcmVOYW1lIiwiZmlsZVBhdGgiLCJnZXRQaHlzaWNhbEZpbGVuYW1lIiwiZ2V0RmlsZW5hbWUiLCJyZXBvcnQiLCJtZXNzYWdlIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsImRlc2NyaXB0aW9uIiwicmVjb21tZW5kZWQiLCJ1cmwiLCJzY2hlbWEiLCJjcmVhdGUiLCJzb3VyY2UiLCJ2YWx1ZSIsImNvbW1vbmpzIl0sIm1hcHBpbmdzIjoiOzs7OztBQUtBLHNEO0FBQ0Esa0U7QUFDQSxxQzs7QUFFQSxTQUFTQSxlQUFULENBQXlCQyxPQUF6QixFQUFrQ0MsSUFBbEMsRUFBd0NDLFdBQXhDLEVBQXFEO0FBQ25ELE1BQU1DLFdBQVdILFFBQVFJLG1CQUFSLEdBQThCSixRQUFRSSxtQkFBUixFQUE5QixHQUE4REosUUFBUUssV0FBUixFQUEvRTs7QUFFQTtBQUNBLE1BQUlGLGFBQWEsUUFBYixJQUF5QkEsYUFBYSwwQkFBUUQsV0FBUixFQUFxQkYsT0FBckIsQ0FBMUMsRUFBeUU7QUFDdkVBLFlBQVFNLE1BQVIsQ0FBZTtBQUNiTCxnQkFEYTtBQUViTSxlQUFTLHdCQUZJLEVBQWY7O0FBSUQ7QUFDRixDLENBbkJEOzs7S0FxQkFDLE9BQU9DLE9BQVAsR0FBaUIsRUFDZkMsTUFBTTtBQUNKQyxVQUFNLFNBREY7QUFFSkMsVUFBTTtBQUNKQyxtQkFBYSx1Q0FEVDtBQUVKQyxtQkFBYSxJQUZUO0FBR0pDLFdBQUssMEJBQVEsZ0JBQVIsQ0FIRCxFQUZGOzs7QUFRSkMsWUFBUSxFQVJKLEVBRFM7O0FBV2ZDLFFBWGUsK0JBV1JqQixPQVhRLEVBV0M7QUFDZCxhQUFPLGdDQUFjLFVBQUNrQixNQUFELEVBQVNqQixJQUFULEVBQWtCO0FBQ3JDRix3QkFBZ0JDLE9BQWhCLEVBQXlCQyxJQUF6QixFQUErQmlCLE9BQU9DLEtBQXRDO0FBQ0QsT0FGTSxFQUVKLEVBQUVDLFVBQVUsSUFBWixFQUZJLENBQVA7QUFHRCxLQWZjLG1CQUFqQiIsImZpbGUiOiJuby1zZWxmLWltcG9ydC5qcyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGZpbGVPdmVydmlldyBGb3JiaWRzIGEgbW9kdWxlIGZyb20gaW1wb3J0aW5nIGl0c2VsZlxuICogQGF1dGhvciBHaW8gZCdBbWVsaW9cbiAqL1xuXG5pbXBvcnQgcmVzb2x2ZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnO1xuaW1wb3J0IG1vZHVsZVZpc2l0b3IgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5mdW5jdGlvbiBpc0ltcG9ydGluZ1NlbGYoY29udGV4dCwgbm9kZSwgcmVxdWlyZU5hbWUpIHtcbiAgY29uc3QgZmlsZVBhdGggPSBjb250ZXh0LmdldFBoeXNpY2FsRmlsZW5hbWUgPyBjb250ZXh0LmdldFBoeXNpY2FsRmlsZW5hbWUoKSA6IGNvbnRleHQuZ2V0RmlsZW5hbWUoKTtcblxuICAvLyBJZiB0aGUgaW5wdXQgaXMgZnJvbSBzdGRpbiwgdGhpcyB0ZXN0IGNhbid0IGZhaWxcbiAgaWYgKGZpbGVQYXRoICE9PSAnPHRleHQ+JyAmJiBmaWxlUGF0aCA9PT0gcmVzb2x2ZShyZXF1aXJlTmFtZSwgY29udGV4dCkpIHtcbiAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICBub2RlLFxuICAgICAgbWVzc2FnZTogJ01vZHVsZSBpbXBvcnRzIGl0c2VsZi4nLFxuICAgIH0pO1xuICB9XG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3Byb2JsZW0nLFxuICAgIGRvY3M6IHtcbiAgICAgIGRlc2NyaXB0aW9uOiAnRm9yYmlkIGEgbW9kdWxlIGZyb20gaW1wb3J0aW5nIGl0c2VsZicsXG4gICAgICByZWNvbW1lbmRlZDogdHJ1ZSxcbiAgICAgIHVybDogZG9jc1VybCgnbm8tc2VsZi1pbXBvcnQnKSxcbiAgICB9LFxuXG4gICAgc2NoZW1hOiBbXSxcbiAgfSxcbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcigoc291cmNlLCBub2RlKSA9PiB7XG4gICAgICBpc0ltcG9ydGluZ1NlbGYoY29udGV4dCwgbm9kZSwgc291cmNlLnZhbHVlKTtcbiAgICB9LCB7IGNvbW1vbmpzOiB0cnVlIH0pO1xuICB9LFxufTtcbiJdfQ==