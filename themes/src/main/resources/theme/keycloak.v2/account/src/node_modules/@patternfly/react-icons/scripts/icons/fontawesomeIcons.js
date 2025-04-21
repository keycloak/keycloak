const { fas } = require('@fortawesome/free-solid-svg-icons');
const { far } = require('@fortawesome/free-regular-svg-icons');
const { fab } = require('@fortawesome/free-brands-svg-icons');

/**
 * @param {object} icon FontAwesome icon to convert
 */
function convertIcon(icon) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [width, height, ligatures, unicode, svgPathData] = icon.icon;

  return {
    xOffset: 0,
    yOffset: 0,
    width,
    height,
    svgPathData
  };
}

/**
 * @param {object} icon FontAwesome icon to name
 * @returns {string} PatternFly name of the icon
 */
function getIconName(icon) {
  if (icon.iconName === '500px') {
    return 'five-hundred-px';
  }
  if (icon.prefix === 'far') {
    return `outlined-${icon.iconName}`;
  }

  return icon.iconName;
}

/**
 * @param {object} icons Icons like { faIconName: { faIconDef } }
 */
function convertIcons(icons) {
  delete icons.faFontAwesomeLogoFull;

  return Object.values(icons)
    .map(icon => ({
      name: getIconName(icon),
      data: convertIcon(icon)
    }))
    .reduce((acc, cur) => {
      acc[cur.name] = cur.data;
      return acc;
    }, {});
}

module.exports = {
  ...convertIcons(fas),
  ...convertIcons(fab),
  ...convertIcons(far)
};
