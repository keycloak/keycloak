/* global global, console */
export function warn(warning, type) {
  if (global && global._YAML_SILENCE_WARNINGS) return;

  var _ref = global && global.process,
      emitWarning = _ref.emitWarning; // This will throw in Jest if `warning` is an Error instance due to
  // https://github.com/facebook/jest/issues/2549


  if (emitWarning) emitWarning(warning, type);else {
    // eslint-disable-next-line no-console
    console.warn(type ? "".concat(type, ": ").concat(warning) : warning);
  }
}
export function warnFileDeprecation(filename) {
  if (global && global._YAML_SILENCE_DEPRECATION_WARNINGS) return;
  var path = filename.replace(/.*yaml[/\\]/i, '').replace(/\.js$/, '').replace(/\\/g, '/');
  warn("The endpoint 'yaml/".concat(path, "' will be removed in a future release."), 'DeprecationWarning');
}
var warned = {};
export function warnOptionDeprecation(name, alternative) {
  if (global && global._YAML_SILENCE_DEPRECATION_WARNINGS) return;
  if (warned[name]) return;
  warned[name] = true;
  var msg = "The option '".concat(name, "' will be removed in a future release");
  msg += alternative ? ", use '".concat(alternative, "' instead.") : '.';
  warn(msg, 'DeprecationWarning');
}