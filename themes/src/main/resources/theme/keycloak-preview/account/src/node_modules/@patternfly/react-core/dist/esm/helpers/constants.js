export const KEY_CODES = {
  ARROW_UP: 38,
  ARROW_DOWN: 40,
  ESCAPE_KEY: 27,
  TAB: 9,
  ENTER: 13,
  SPACE: 32
};
export const SIDE = {
  RIGHT: 'right',
  LEFT: 'left',
  BOTH: 'both',
  NONE: 'none'
};
export const KEYHANDLER_DIRECTION = {
  UP: 'up',
  DOWN: 'down',
  RIGHT: 'right',
  LEFT: 'left'
};
export let ValidatedOptions;

(function (ValidatedOptions) {
  ValidatedOptions["success"] = "success";
  ValidatedOptions["error"] = "error";
  ValidatedOptions["default"] = "default";
})(ValidatedOptions || (ValidatedOptions = {}));
//# sourceMappingURL=constants.js.map