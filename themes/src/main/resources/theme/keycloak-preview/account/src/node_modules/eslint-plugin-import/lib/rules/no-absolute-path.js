'use strict';

var _moduleVisitor = require('eslint-module-utils/moduleVisitor');

var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);

var _importType = require('../core/importType');

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-absolute-path')
    },
    schema: [(0, _moduleVisitor.makeOptionsSchema)()]
  },

  create: function (context) {
    function reportIfAbsolute(source) {
      if ((0, _importType.isAbsolute)(source.value)) {
        context.report(source, 'Do not import modules using an absolute path');
      }
    }

    const options = Object.assign({ esmodule: true, commonjs: true }, context.options[0]);
    return (0, _moduleVisitor2.default)(reportIfAbsolute, options);
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLWFic29sdXRlLXBhdGguanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJyZXBvcnRJZkFic29sdXRlIiwic291cmNlIiwidmFsdWUiLCJyZXBvcnQiLCJvcHRpb25zIiwiT2JqZWN0IiwiYXNzaWduIiwiZXNtb2R1bGUiLCJjb21tb25qcyJdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7OztBQUNBOztBQUNBOzs7Ozs7QUFFQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssdUJBQVEsa0JBQVI7QUFERCxLQUZGO0FBS0pDLFlBQVEsQ0FBRSx1Q0FBRjtBQUxKLEdBRFM7O0FBU2ZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjtBQUN6QixhQUFTQyxnQkFBVCxDQUEwQkMsTUFBMUIsRUFBa0M7QUFDaEMsVUFBSSw0QkFBV0EsT0FBT0MsS0FBbEIsQ0FBSixFQUE4QjtBQUM1QkgsZ0JBQVFJLE1BQVIsQ0FBZUYsTUFBZixFQUF1Qiw4Q0FBdkI7QUFDRDtBQUNGOztBQUVELFVBQU1HLFVBQVVDLE9BQU9DLE1BQVAsQ0FBYyxFQUFFQyxVQUFVLElBQVosRUFBa0JDLFVBQVUsSUFBNUIsRUFBZCxFQUFrRFQsUUFBUUssT0FBUixDQUFnQixDQUFoQixDQUFsRCxDQUFoQjtBQUNBLFdBQU8sNkJBQWNKLGdCQUFkLEVBQWdDSSxPQUFoQyxDQUFQO0FBQ0Q7QUFsQmMsQ0FBakIiLCJmaWxlIjoicnVsZXMvbm8tYWJzb2x1dGUtcGF0aC5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBtb2R1bGVWaXNpdG9yLCB7IG1ha2VPcHRpb25zU2NoZW1hIH0gZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJ1xuaW1wb3J0IHsgaXNBYnNvbHV0ZSB9IGZyb20gJy4uL2NvcmUvaW1wb3J0VHlwZSdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tYWJzb2x1dGUtcGF0aCcpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbIG1ha2VPcHRpb25zU2NoZW1hKCkgXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgZnVuY3Rpb24gcmVwb3J0SWZBYnNvbHV0ZShzb3VyY2UpIHtcbiAgICAgIGlmIChpc0Fic29sdXRlKHNvdXJjZS52YWx1ZSkpIHtcbiAgICAgICAgY29udGV4dC5yZXBvcnQoc291cmNlLCAnRG8gbm90IGltcG9ydCBtb2R1bGVzIHVzaW5nIGFuIGFic29sdXRlIHBhdGgnKVxuICAgICAgfVxuICAgIH1cblxuICAgIGNvbnN0IG9wdGlvbnMgPSBPYmplY3QuYXNzaWduKHsgZXNtb2R1bGU6IHRydWUsIGNvbW1vbmpzOiB0cnVlIH0sIGNvbnRleHQub3B0aW9uc1swXSlcbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcihyZXBvcnRJZkFic29sdXRlLCBvcHRpb25zKVxuICB9LFxufVxuIl19