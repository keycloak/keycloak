'use strict';

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

var _sumBy = require('lodash/sumBy');

var _sumBy2 = _interopRequireDefault(_sumBy);

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _moduleVisitor = require('eslint-module-utils/moduleVisitor');

var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * convert a potentially relative path from node utils into a true
 * relative path.
 *
 * ../ -> ..
 * ./ -> .
 * .foo/bar -> ./.foo/bar
 * ..foo/bar -> ./..foo/bar
 * foo/bar -> ./foo/bar
 *
 * @param rel {string} relative posix path potentially missing leading './'
 * @returns {string} relative posix path that always starts with a ./
 **/
function toRel(rel) {
  const stripped = rel.replace(/\/$/g, '');
  return (/^((\.\.)|(\.))($|\/)/.test(stripped) ? stripped : `./${stripped}`
  );
} /**
   * @fileOverview Ensures that there are no useless path segments
   * @author Thomas Grainger
   */

function normalize(fn) {
  return toRel(_path2.default.posix.normalize(fn));
}

const countRelParent = x => (0, _sumBy2.default)(x, v => v === '..');

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-useless-path-segments')
    },

    schema: [{
      type: 'object',
      properties: {
        commonjs: { type: 'boolean' }
      },
      additionalProperties: false
    }],

    fixable: 'code'
  },

  create: function (context) {
    const currentDir = _path2.default.dirname(context.getFilename());

    function checkSourceValue(source) {
      const value = source.value;


      function report(proposed) {
        context.report({
          node: source,
          message: `Useless path segments for "${value}", should be "${proposed}"`,
          fix: fixer => fixer.replaceText(source, JSON.stringify(proposed))
        });
      }

      if (!value.startsWith('.')) {
        return;
      }

      const resolvedPath = (0, _resolve2.default)(value, context);
      const normed = normalize(value);
      if (normed !== value && resolvedPath === (0, _resolve2.default)(normed, context)) {
        return report(normed);
      }

      if (value.startsWith('./')) {
        return;
      }

      if (resolvedPath === undefined) {
        return;
      }

      const expected = _path2.default.relative(currentDir, resolvedPath);
      const expectedSplit = expected.split(_path2.default.sep);
      const valueSplit = value.replace(/^\.\//, '').split('/');
      const valueNRelParents = countRelParent(valueSplit);
      const expectedNRelParents = countRelParent(expectedSplit);
      const diff = valueNRelParents - expectedNRelParents;

      if (diff <= 0) {
        return;
      }

      return report(toRel(valueSplit.slice(0, expectedNRelParents).concat(valueSplit.slice(valueNRelParents + diff)).join('/')));
    }

    return (0, _moduleVisitor2.default)(checkSourceValue, context.options[0]);
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLXVzZWxlc3MtcGF0aC1zZWdtZW50cy5qcyJdLCJuYW1lcyI6WyJ0b1JlbCIsInJlbCIsInN0cmlwcGVkIiwicmVwbGFjZSIsInRlc3QiLCJub3JtYWxpemUiLCJmbiIsInBhdGgiLCJwb3NpeCIsImNvdW50UmVsUGFyZW50IiwieCIsInYiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwicHJvcGVydGllcyIsImNvbW1vbmpzIiwiYWRkaXRpb25hbFByb3BlcnRpZXMiLCJmaXhhYmxlIiwiY3JlYXRlIiwiY29udGV4dCIsImN1cnJlbnREaXIiLCJkaXJuYW1lIiwiZ2V0RmlsZW5hbWUiLCJjaGVja1NvdXJjZVZhbHVlIiwic291cmNlIiwidmFsdWUiLCJyZXBvcnQiLCJwcm9wb3NlZCIsIm5vZGUiLCJtZXNzYWdlIiwiZml4IiwiZml4ZXIiLCJyZXBsYWNlVGV4dCIsIkpTT04iLCJzdHJpbmdpZnkiLCJzdGFydHNXaXRoIiwicmVzb2x2ZWRQYXRoIiwibm9ybWVkIiwidW5kZWZpbmVkIiwiZXhwZWN0ZWQiLCJyZWxhdGl2ZSIsImV4cGVjdGVkU3BsaXQiLCJzcGxpdCIsInNlcCIsInZhbHVlU3BsaXQiLCJ2YWx1ZU5SZWxQYXJlbnRzIiwiZXhwZWN0ZWROUmVsUGFyZW50cyIsImRpZmYiLCJzbGljZSIsImNvbmNhdCIsImpvaW4iLCJvcHRpb25zIl0sIm1hcHBpbmdzIjoiOztBQUtBOzs7O0FBQ0E7Ozs7QUFDQTs7OztBQUNBOzs7O0FBQ0E7Ozs7OztBQUVBOzs7Ozs7Ozs7Ozs7O0FBYUEsU0FBU0EsS0FBVCxDQUFlQyxHQUFmLEVBQW9CO0FBQ2xCLFFBQU1DLFdBQVdELElBQUlFLE9BQUosQ0FBWSxNQUFaLEVBQW9CLEVBQXBCLENBQWpCO0FBQ0EsU0FBTyx3QkFBdUJDLElBQXZCLENBQTRCRixRQUE1QixJQUF3Q0EsUUFBeEMsR0FBb0QsS0FBSUEsUUFBUztBQUF4RTtBQUNELEMsQ0Ez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IEBmaWxlT3ZlcnZpZXcgRW5zdXJlcyB0aGF0IHRoZXJlIGFyZSBubyB1c2VsZXNzIHBhdGggc2VnbWVudHNcbiAqIEBhdXRob3IgVGhvbWFzIEdyYWluZ2VyXG4gKi9cblxuaW1wb3J0IHBhdGggZnJvbSAncGF0aCdcbmltcG9ydCBzdW1CeSBmcm9tICdsb2Rhc2gvc3VtQnknXG5pbXBvcnQgcmVzb2x2ZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnXG5pbXBvcnQgbW9kdWxlVmlzaXRvciBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL21vZHVsZVZpc2l0b3InXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG4vKipcbiAqIGNvbnZlcnQgYSBwb3RlbnRpYWxseSByZWxhdGl2ZSBwYXRoIGZyb20gbm9kZSB1dGlscyBpbnRvIGEgdHJ1ZVxuICogcmVsYXRpdmUgcGF0aC5cbiAqXG4gKiAuLi8gLT4gLi5cbiAqIC4vIC0+IC5cbiAqIC5mb28vYmFyIC0+IC4vLmZvby9iYXJcbiAqIC4uZm9vL2JhciAtPiAuLy4uZm9vL2JhclxuICogZm9vL2JhciAtPiAuL2Zvby9iYXJcbiAqXG4gKiBAcGFyYW0gcmVsIHtzdHJpbmd9IHJlbGF0aXZlIHBvc2l4IHBhdGggcG90ZW50aWFsbHkgbWlzc2luZyBsZWFkaW5nICcuLydcbiAqIEByZXR1cm5zIHtzdHJpbmd9IHJlbGF0aXZlIHBvc2l4IHBhdGggdGhhdCBhbHdheXMgc3RhcnRzIHdpdGggYSAuL1xuICoqL1xuZnVuY3Rpb24gdG9SZWwocmVsKSB7XG4gIGNvbnN0IHN0cmlwcGVkID0gcmVsLnJlcGxhY2UoL1xcLyQvZywgJycpXG4gIHJldHVybiAvXigoXFwuXFwuKXwoXFwuKSkoJHxcXC8pLy50ZXN0KHN0cmlwcGVkKSA/IHN0cmlwcGVkIDogYC4vJHtzdHJpcHBlZH1gXG59XG5cbmZ1bmN0aW9uIG5vcm1hbGl6ZShmbikge1xuICByZXR1cm4gdG9SZWwocGF0aC5wb3NpeC5ub3JtYWxpemUoZm4pKVxufVxuXG5jb25zdCBjb3VudFJlbFBhcmVudCA9IHggPT4gc3VtQnkoeCwgdiA9PiB2ID09PSAnLi4nKVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLXVzZWxlc3MtcGF0aC1zZWdtZW50cycpLFxuICAgIH0sXG5cbiAgICBzY2hlbWE6IFtcbiAgICAgIHtcbiAgICAgICAgdHlwZTogJ29iamVjdCcsXG4gICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICBjb21tb25qczogeyB0eXBlOiAnYm9vbGVhbicgfSxcbiAgICAgICAgfSxcbiAgICAgICAgYWRkaXRpb25hbFByb3BlcnRpZXM6IGZhbHNlLFxuICAgICAgfSxcbiAgICBdLFxuXG4gICAgZml4YWJsZTogJ2NvZGUnLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICBjb25zdCBjdXJyZW50RGlyID0gcGF0aC5kaXJuYW1lKGNvbnRleHQuZ2V0RmlsZW5hbWUoKSlcblxuICAgIGZ1bmN0aW9uIGNoZWNrU291cmNlVmFsdWUoc291cmNlKSB7XG4gICAgICBjb25zdCB7IHZhbHVlIH0gPSBzb3VyY2VcblxuICAgICAgZnVuY3Rpb24gcmVwb3J0KHByb3Bvc2VkKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlOiBzb3VyY2UsXG4gICAgICAgICAgbWVzc2FnZTogYFVzZWxlc3MgcGF0aCBzZWdtZW50cyBmb3IgXCIke3ZhbHVlfVwiLCBzaG91bGQgYmUgXCIke3Byb3Bvc2VkfVwiYCxcbiAgICAgICAgICBmaXg6IGZpeGVyID0+IGZpeGVyLnJlcGxhY2VUZXh0KHNvdXJjZSwgSlNPTi5zdHJpbmdpZnkocHJvcG9zZWQpKSxcbiAgICAgICAgfSlcbiAgICAgIH1cblxuICAgICAgaWYgKCF2YWx1ZS5zdGFydHNXaXRoKCcuJykpIHtcbiAgICAgICAgcmV0dXJuXG4gICAgICB9XG5cbiAgICAgIGNvbnN0IHJlc29sdmVkUGF0aCA9IHJlc29sdmUodmFsdWUsIGNvbnRleHQpXG4gICAgICBjb25zdCBub3JtZWQgPSBub3JtYWxpemUodmFsdWUpXG4gICAgICBpZiAobm9ybWVkICE9PSB2YWx1ZSAmJiByZXNvbHZlZFBhdGggPT09IHJlc29sdmUobm9ybWVkLCBjb250ZXh0KSkge1xuICAgICAgICByZXR1cm4gcmVwb3J0KG5vcm1lZClcbiAgICAgIH1cblxuICAgICAgaWYgKHZhbHVlLnN0YXJ0c1dpdGgoJy4vJykpIHtcbiAgICAgICAgcmV0dXJuXG4gICAgICB9XG5cbiAgICAgIGlmIChyZXNvbHZlZFBhdGggPT09IHVuZGVmaW5lZCkge1xuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgY29uc3QgZXhwZWN0ZWQgPSBwYXRoLnJlbGF0aXZlKGN1cnJlbnREaXIsIHJlc29sdmVkUGF0aClcbiAgICAgIGNvbnN0IGV4cGVjdGVkU3BsaXQgPSBleHBlY3RlZC5zcGxpdChwYXRoLnNlcClcbiAgICAgIGNvbnN0IHZhbHVlU3BsaXQgPSB2YWx1ZS5yZXBsYWNlKC9eXFwuXFwvLywgJycpLnNwbGl0KCcvJylcbiAgICAgIGNvbnN0IHZhbHVlTlJlbFBhcmVudHMgPSBjb3VudFJlbFBhcmVudCh2YWx1ZVNwbGl0KVxuICAgICAgY29uc3QgZXhwZWN0ZWROUmVsUGFyZW50cyA9IGNvdW50UmVsUGFyZW50KGV4cGVjdGVkU3BsaXQpXG4gICAgICBjb25zdCBkaWZmID0gdmFsdWVOUmVsUGFyZW50cyAtIGV4cGVjdGVkTlJlbFBhcmVudHNcblxuICAgICAgaWYgKGRpZmYgPD0gMCkge1xuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgcmV0dXJuIHJlcG9ydChcbiAgICAgICAgdG9SZWwodmFsdWVTcGxpdFxuICAgICAgICAgIC5zbGljZSgwLCBleHBlY3RlZE5SZWxQYXJlbnRzKVxuICAgICAgICAgIC5jb25jYXQodmFsdWVTcGxpdC5zbGljZSh2YWx1ZU5SZWxQYXJlbnRzICsgZGlmZikpXG4gICAgICAgICAgLmpvaW4oJy8nKSlcbiAgICAgIClcbiAgICB9XG5cbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcihjaGVja1NvdXJjZVZhbHVlLCBjb250ZXh0Lm9wdGlvbnNbMF0pXG4gIH0sXG59XG4iXX0=