const faIcons = require('./icons/fontawesomeIcons');
const patternflyIcons = require('./icons/patternflyIcons');
const customIcons = require('./icons/customIcons');

/**
 * Generates icons from FontAwesome, PatternFly CSS, and custom icons in this repo.
 */
function generateIcons() {
  return {
    ...faIcons,
    ...patternflyIcons,
    ...customIcons
  };
}

module.exports = {
  generateIcons
};
