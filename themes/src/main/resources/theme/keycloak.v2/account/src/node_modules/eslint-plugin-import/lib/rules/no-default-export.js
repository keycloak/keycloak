'use strict';var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-default-export') },

    schema: [] },


  create: function () {function create(context) {
      // ignore non-modules
      if (context.parserOptions.sourceType !== 'module') {
        return {};
      }

      var preferNamed = 'Prefer named exports.';
      var noAliasDefault = function () {function noAliasDefault(_ref) {var local = _ref.local;return 'Do not alias `' + String(local.name) + '` as `default`. Just export `' + String(local.name) + '` itself instead.';}return noAliasDefault;}();

      return {
        ExportDefaultDeclaration: function () {function ExportDefaultDeclaration(node) {var _ref2 =
            context.getSourceCode().getFirstTokens(node)[1] || {},loc = _ref2.loc;
            context.report({ node: node, message: preferNamed, loc: loc });
          }return ExportDefaultDeclaration;}(),

        ExportNamedDeclaration: function () {function ExportNamedDeclaration(node) {
            node.specifiers.filter(function (specifier) {return (specifier.exported.name || specifier.exported.value) === 'default';}).forEach(function (specifier) {var _ref3 =
              context.getSourceCode().getFirstTokens(node)[1] || {},loc = _ref3.loc;
              if (specifier.type === 'ExportDefaultSpecifier') {
                context.report({ node: node, message: preferNamed, loc: loc });
              } else if (specifier.type === 'ExportSpecifier') {
                context.report({ node: node, message: noAliasDefault(specifier), loc: loc });
              }
            });
          }return ExportNamedDeclaration;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1kZWZhdWx0LWV4cG9ydC5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwiY29udGV4dCIsInBhcnNlck9wdGlvbnMiLCJzb3VyY2VUeXBlIiwicHJlZmVyTmFtZWQiLCJub0FsaWFzRGVmYXVsdCIsImxvY2FsIiwibmFtZSIsIkV4cG9ydERlZmF1bHREZWNsYXJhdGlvbiIsIm5vZGUiLCJnZXRTb3VyY2VDb2RlIiwiZ2V0Rmlyc3RUb2tlbnMiLCJsb2MiLCJyZXBvcnQiLCJtZXNzYWdlIiwiRXhwb3J0TmFtZWREZWNsYXJhdGlvbiIsInNwZWNpZmllcnMiLCJmaWx0ZXIiLCJzcGVjaWZpZXIiLCJleHBvcnRlZCIsInZhbHVlIiwiZm9yRWFjaCJdLCJtYXBwaW5ncyI6ImFBQUEscUM7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLDBCQUFRLG1CQUFSLENBREQsRUFGRjs7QUFLSkMsWUFBUSxFQUxKLEVBRFM7OztBQVNmQyxRQVRlLCtCQVNSQyxPQVRRLEVBU0M7QUFDZDtBQUNBLFVBQUlBLFFBQVFDLGFBQVIsQ0FBc0JDLFVBQXRCLEtBQXFDLFFBQXpDLEVBQW1EO0FBQ2pELGVBQU8sRUFBUDtBQUNEOztBQUVELFVBQU1DLGNBQWMsdUJBQXBCO0FBQ0EsVUFBTUMsOEJBQWlCLFNBQWpCQSxjQUFpQixZQUFHQyxLQUFILFFBQUdBLEtBQUgsa0NBQWlDQSxNQUFNQyxJQUF2Qyw2Q0FBK0VELE1BQU1DLElBQXJGLHlCQUFqQix5QkFBTjs7QUFFQSxhQUFPO0FBQ0xDLGdDQURLLGlEQUNvQkMsSUFEcEIsRUFDMEI7QUFDYlIsb0JBQVFTLGFBQVIsR0FBd0JDLGNBQXhCLENBQXVDRixJQUF2QyxFQUE2QyxDQUE3QyxLQUFtRCxFQUR0QyxDQUNyQkcsR0FEcUIsU0FDckJBLEdBRHFCO0FBRTdCWCxvQkFBUVksTUFBUixDQUFlLEVBQUVKLFVBQUYsRUFBUUssU0FBU1YsV0FBakIsRUFBOEJRLFFBQTlCLEVBQWY7QUFDRCxXQUpJOztBQU1MRyw4QkFOSywrQ0FNa0JOLElBTmxCLEVBTXdCO0FBQzNCQSxpQkFBS08sVUFBTCxDQUFnQkMsTUFBaEIsQ0FBdUIsNkJBQWEsQ0FBQ0MsVUFBVUMsUUFBVixDQUFtQlosSUFBbkIsSUFBMkJXLFVBQVVDLFFBQVYsQ0FBbUJDLEtBQS9DLE1BQTBELFNBQXZFLEVBQXZCLEVBQXlHQyxPQUF6RyxDQUFpSCxxQkFBYTtBQUM1R3BCLHNCQUFRUyxhQUFSLEdBQXdCQyxjQUF4QixDQUF1Q0YsSUFBdkMsRUFBNkMsQ0FBN0MsS0FBbUQsRUFEeUQsQ0FDcEhHLEdBRG9ILFNBQ3BIQSxHQURvSDtBQUU1SCxrQkFBSU0sVUFBVXRCLElBQVYsS0FBbUIsd0JBQXZCLEVBQWlEO0FBQy9DSyx3QkFBUVksTUFBUixDQUFlLEVBQUVKLFVBQUYsRUFBUUssU0FBU1YsV0FBakIsRUFBOEJRLFFBQTlCLEVBQWY7QUFDRCxlQUZELE1BRU8sSUFBSU0sVUFBVXRCLElBQVYsS0FBbUIsaUJBQXZCLEVBQTBDO0FBQy9DSyx3QkFBUVksTUFBUixDQUFlLEVBQUVKLFVBQUYsRUFBUUssU0FBU1QsZUFBZWEsU0FBZixDQUFqQixFQUE0Q04sUUFBNUMsRUFBZjtBQUNEO0FBQ0YsYUFQRDtBQVFELFdBZkksbUNBQVA7O0FBaUJELEtBbkNjLG1CQUFqQiIsImZpbGUiOiJuby1kZWZhdWx0LWV4cG9ydC5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLWRlZmF1bHQtZXhwb3J0JyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuXG4gIGNyZWF0ZShjb250ZXh0KSB7XG4gICAgLy8gaWdub3JlIG5vbi1tb2R1bGVzXG4gICAgaWYgKGNvbnRleHQucGFyc2VyT3B0aW9ucy5zb3VyY2VUeXBlICE9PSAnbW9kdWxlJykge1xuICAgICAgcmV0dXJuIHt9O1xuICAgIH1cblxuICAgIGNvbnN0IHByZWZlck5hbWVkID0gJ1ByZWZlciBuYW1lZCBleHBvcnRzLic7XG4gICAgY29uc3Qgbm9BbGlhc0RlZmF1bHQgPSAoeyBsb2NhbCB9KSA9PiBgRG8gbm90IGFsaWFzIFxcYCR7bG9jYWwubmFtZX1cXGAgYXMgXFxgZGVmYXVsdFxcYC4gSnVzdCBleHBvcnQgXFxgJHtsb2NhbC5uYW1lfVxcYCBpdHNlbGYgaW5zdGVhZC5gO1xuXG4gICAgcmV0dXJuIHtcbiAgICAgIEV4cG9ydERlZmF1bHREZWNsYXJhdGlvbihub2RlKSB7XG4gICAgICAgIGNvbnN0IHsgbG9jIH0gPSBjb250ZXh0LmdldFNvdXJjZUNvZGUoKS5nZXRGaXJzdFRva2Vucyhub2RlKVsxXSB8fCB7fTtcbiAgICAgICAgY29udGV4dC5yZXBvcnQoeyBub2RlLCBtZXNzYWdlOiBwcmVmZXJOYW1lZCwgbG9jIH0pO1xuICAgICAgfSxcblxuICAgICAgRXhwb3J0TmFtZWREZWNsYXJhdGlvbihub2RlKSB7XG4gICAgICAgIG5vZGUuc3BlY2lmaWVycy5maWx0ZXIoc3BlY2lmaWVyID0+IChzcGVjaWZpZXIuZXhwb3J0ZWQubmFtZSB8fCBzcGVjaWZpZXIuZXhwb3J0ZWQudmFsdWUpID09PSAnZGVmYXVsdCcpLmZvckVhY2goc3BlY2lmaWVyID0+IHtcbiAgICAgICAgICBjb25zdCB7IGxvYyB9ID0gY29udGV4dC5nZXRTb3VyY2VDb2RlKCkuZ2V0Rmlyc3RUb2tlbnMobm9kZSlbMV0gfHwge307XG4gICAgICAgICAgaWYgKHNwZWNpZmllci50eXBlID09PSAnRXhwb3J0RGVmYXVsdFNwZWNpZmllcicpIHtcbiAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KHsgbm9kZSwgbWVzc2FnZTogcHJlZmVyTmFtZWQsIGxvYyB9KTtcbiAgICAgICAgICB9IGVsc2UgaWYgKHNwZWNpZmllci50eXBlID09PSAnRXhwb3J0U3BlY2lmaWVyJykge1xuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoeyBub2RlLCBtZXNzYWdlOiBub0FsaWFzRGVmYXVsdChzcGVjaWZpZXIpLCBsb2MgIH0pO1xuICAgICAgICAgIH1cbiAgICAgICAgfSk7XG4gICAgICB9LFxuICAgIH07XG4gIH0sXG59O1xuIl19