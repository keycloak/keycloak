const {
  save,
  'folder-open': folderOpen,
  edit,
  print,
  spinner,
  home,
  history,
  memory,
  server,
  user,
  users,
  info,
  filter,
  key,
  ...icons
} = require('@patternfly/patternfly/icons/pf-icons');

module.exports = {
  pfIcons: {
    'save-alt': save,
    'folder-open-alt': folderOpen,
    'edit-alt': edit,
    'print-alt': print,
    'spinner-alt': spinner,
    'home-alt': home,
    'memory-alt': memory,
    'server-alt': server,
    'user-sec': user,
    'users-alt': users,
    'info-alt': info,
    'filter-alt': filter,
    ...icons
  }
};
