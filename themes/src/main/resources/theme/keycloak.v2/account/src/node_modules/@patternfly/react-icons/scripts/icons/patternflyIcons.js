const pfIcons = require('@patternfly/patternfly/icons/pf-icons.json');

const renaming = {
  save: 'save-alt',
  'folder-open': 'folder-open-alt',
  edit: 'edit-alt',
  print: 'print-alt',
  spinner: 'spinner-alt',
  home: 'home-alt',
  memory: 'memory-alt',
  server: 'server-alt',
  user: 'user-sec',
  users: 'users-alt',
  info: 'info-alt',
  filter: 'filter-alt'
};

/**
 * @param {object} icons Icons like { screen: { "width": 1024, "height": 1024, "svgPathData": "" } }
 */
function convertIcons(icons) {
  delete icons.history;

  Object.entries(renaming).forEach(([oldKey, newKey]) => {
    icons[newKey] = icons[oldKey];
    delete icons[oldKey];
  });

  return icons;
}

module.exports = convertIcons(pfIcons);
