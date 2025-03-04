console = {
  log: print,
  warn: print,
  error: print,
};

var forEach = Array.prototype.forEach;
var implicitReverseMap = [
  { key: 'alerts:read', value: 'resources:manage' },
  { key: 'alerts:read', value: 'vumodule:modifySources' },
  { key: 'alerts:read', value: 'vumodule:write' },
  { key: 'dashboards:admin', value: 'dataSource:manage' },
  { key: 'dashboards:write', value: 'dashboards:admin' },
  { key: 'dataModel:read', value: 'alerts:read' },
  { key: 'dataModel:read', value: 'alerts:write' },
  { key: 'dataModel:read', value: 'dashboards:admin' },
  { key: 'dataModel:read', value: 'dashboards:write' },
  { key: 'dataModel:read', value: 'dataExtraction:read' },
  { key: 'dataModel:read', value: 'dataExtraction:write' },
  { key: 'dataModel:read', value: 'dataModel:manageDataStore' },
  { key: 'dataModel:read', value: 'dataSource:manage' },
  { key: 'dataModel:read', value: 'insights:read' },
  { key: 'dataModel:read', value: 'insights:write' },
  { key: 'dataModel:read', value: 'mobileDashboards:read' },
  { key: 'dataModel:read', value: 'mobileDashboards:write' },
  { key: 'dataModel:read', value: 'reports:read' },
  { key: 'dataModel:read', value: 'reports:write' },
  { key: 'dataModel:read', value: 'resources:manage' },
  { key: 'dataModel:read', value: 'utm:read' },
  { key: 'dataModel:read', value: 'utm:write' },
  { key: 'dataModel:read', value: 'vumodule:write' },
  { key: 'dataModel:write', value: 'dataModel:manageDataStore' },
  { key: 'dataSource:manage', value: 'dashboards:admin' },
  { key: 'definitions:read', value: 'alerts:read' },
  { key: 'definitions:read', value: 'alerts:write' },
  { key: 'definitions:read', value: 'reports:read' },
  { key: 'definitions:read', value: 'reports:write' },
  { key: 'definitions:read', value: 'vumodule:modifySources' },
  { key: 'definitions:read', value: 'vumodule:read' },
  { key: 'definitions:read', value: 'vumodule:write' },
  { key: 'insights:read', value: 'dashboards:admin' },
  { key: 'insights:read', value: 'dashboards:write' },
  { key: 'insights:read', value: 'dataSource:manage' },
  { key: 'insights:read', value: 'mobileDashboards:read' },
  { key: 'insights:read', value: 'mobileDashboards:write' },
  { key: 'insights:read', value: 'resources:manage' },
  { key: 'insights:read', value: 'insights:modifySystemResources' },
  { key: 'insights:write', value: 'insights:modifySystemResources' },
  { key: 'preferences:read', value: 'alerts:read' },
  { key: 'preferences:read', value: 'alerts:write' },
  { key: 'preferences:read', value: 'events:read' },
  { key: 'preferences:read', value: 'events:write' },
  { key: 'preferences:read', value: 'reports:read' },
  { key: 'preferences:read', value: 'reports:write' },
  { key: 'users:read', value: 'events:write' },
  { key: 'utm:read', value: 'dashboards:admin' },
  { key: 'utm:read', value: 'dashboards:write' },
  { key: 'utm:read', value: 'dataSource:manage' },
  { key: 'utm:read', value: 'resources:manage' },
  { key: 'vumodule:modify_sources', value: 'vumodule:write' },
  { key: 'vumodule:read', value: 'vumodule:modifySources' },
  { key: 'dataModel:read:read', value: 'vumodule:modifySources' },
];

function buildPrivilege(resource, scopeName) {
  var resourceName = resource.getName();
  var builtPrivilege = resourceName + ':' + scopeName;
  return builtPrivilege;
}

// by default, grants any permission associated with this policy
var context = $evaluation.getContext();
var permission = $evaluation.getPermission();

var identity = context.getIdentity();
var attributes = identity.getAttributes();

if (identity.hasRealmRole('admin')) {
  console.log('granting all permissions for admin user ' + identity.getId());
  $evaluation.grant();
}

if (attributes.exists('privilege') === false) {
  console.log('No privileges found for user ' + identity.getId());
  $evaluation.denyIfNoEffect();
}

var resource = permission.getResource();
var scopes = permission.getScopes().toArray();

scope = scopes[0];

var requiredPrivilege = buildPrivilege(resource, scope.getName());

function toUrn(rs) {
  return 'vrn:vusmartmaps:resources:' + rs;
}

if (attributes.containsValue('privilege', requiredPrivilege)) {
  $evaluation.grant();
} else {
  var scopeName = scope.getName();
  var hasWritePermission = attributes.containsValue(
    'privilege',
    buildPrivilege(resource, 'write')
  );
  if (scopeName === 'read' && hasWritePermission) {
    $evaluation.grant();
  }
  forEach.call(implicitReverseMap, function (v) {
    if (
      toUrn(v.key) === requiredPrivilege &&
      attributes.containsValue('privilege', toUrn(v.value))
    ) {
      $evaluation.grant();
      console.log('Granted ' + v.key + ' from ' + v.value);
    }
  });
}

$evaluation.denyIfNoEffect();
