const { fas } = require('@fortawesome/free-solid-svg-icons');
const { far } = require('@fortawesome/free-regular-svg-icons');
const { fab } = require('@fortawesome/free-brands-svg-icons');
const { pfIcons } = require('./pfIcons');
const { custom } = require('./customIcons');
module.exports = {
  fontAwesome: {
    solid: Object.keys(fas),
    regular: Object.keys(far),
    brands: Object.keys(fab)
      .map(icon => {
        if (icon.indexOf('500') !== -1) {
          return {
            title: 'fiveHundredPx',
            name: 'fa500px'
          };
        }
        return icon;
      })
      .filter(icon => icon !== 'faFontAwesomeLogoFull')
  },
  custom,
  pfIcons
};
