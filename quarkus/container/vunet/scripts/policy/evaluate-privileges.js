var logger = org.jboss.logging.Logger.getLogger("vunet.policy.evaluate-privileges");
var Collectors = java.util.stream.Collectors;
var HashMap = java.util.HashMap;
var HashSet = java.util.HashSet;

// Implicit privilege mappings: granting privilege -> array of privileges it grants
var implicitPrivilegeMappings = {
  'alerts:read': ['dataModel:read', 'definitions:read', 'preferences:read'],
  'alerts:write': ['dataModel:read', 'definitions:read', 'preferences:read'],
  'dashboards:admin': ['dashboards:write', 'dataModel:read', 'insights:read', 'utm:read', 'dataSource:manage'],
  'dashboards:write': ['dataModel:read', 'insights:read', 'utm:read'],
  'dataExtraction:read': ['dataModel:read'],
  'dataExtraction:write': ['dataModel:read'],
  'dataModel:manageDataStore': ['dataModel:read', 'dataModel:write'],
  'dataModel:write': ['dataModel:read'],
  'dataSource:manage': ['dashboards:admin', 'dataModel:read', 'insights:read', 'utm:read'],
  'events:read': ['preferences:read'],
  'events:write': ['preferences:read', 'users:read'],
  'insights:modifySystemResources': ['insights:read', 'insights:write'],
  'insights:read': ['dataModel:read'],
  'insights:write': ['dataModel:read'],
  'mobileDashboards:read': ['dataModel:read', 'insights:read'],
  'mobileDashboards:write': ['dataModel:read', 'insights:read'],
  'reports:read': ['dataModel:read', 'definitions:read', 'preferences:read'],
  'reports:write': ['dataModel:read', 'definitions:read', 'preferences:read'],
  'resources:manage': ['alerts:read', 'dataModel:read', 'insights:read', 'utm:read'],
  'utm:read': ['dataModel:read'],
  'utm:write': ['dataModel:read'],
  'vumodule:modifySources': ['alerts:read', 'definitions:read', 'vumodule:read', 'dataModel:read'],
  'vumodule:read': ['definitions:read'],
  'vumodule:write': ['alerts:read', 'dataModel:read', 'definitions:read', 'vumodule:modify_sources']
};

// Convert to Java HashMap for better performance
var implicitPrivilegeMap = new HashMap();
for (var grantingPrivilege in implicitPrivilegeMappings) {
  var grantedPrivileges = new HashSet();
  for (var i = 0; i < implicitPrivilegeMappings[grantingPrivilege].length; i++) {
    grantedPrivileges.add(implicitPrivilegeMappings[grantingPrivilege][i]);
  }
  implicitPrivilegeMap.put(grantingPrivilege, grantedPrivileges);
}

var URN_PREFIX = 'vrn:vusmartmaps:resources:';

function toUrn(rs) {
  return URN_PREFIX + rs;
}

function fromUrn(urn) {
  if (urn.startsWith(URN_PREFIX)) {
    return urn.substring(URN_PREFIX.length);
  }
  return urn; // return as-is if not a URN
}
// Get explicit privileges from user groups
function _getExplicitPrivileges(user) {
  return user.getGroupsStream()
    .flatMap(function (group) {
      return group.getAttributeStream("privilege");
    })
    .collect(Collectors.toSet());
}

// Extend explicit privileges with implicit privileges
function _extendExplicitPrivileges(explicitPrivileges) {
  var extendedPrivileges = new HashSet(explicitPrivileges);
  var iterator = explicitPrivileges.iterator();

  while (iterator.hasNext()) {
    var privilegeUrn = iterator.next();
    var privilege = fromUrn(privilegeUrn); // Strip URN prefix for lookup
    var splitPrivilege = privilege.split(':');
    var resource = splitPrivilege[0];
    var scope = splitPrivilege[1];

    // automatically assign read scope if write scope is present
    if (scope === "write") {
      extendedPrivileges.add(toUrn(resource + ":read"));
    }

    // Check if this privilege grants any other privileges
    var grantedPrivileges = implicitPrivilegeMap.get(privilege);
    if (grantedPrivileges !== null) {
      var grantedIterator = grantedPrivileges.iterator();
      while (grantedIterator.hasNext()) {
        var grantedPrivilege = grantedIterator.next();
        extendedPrivileges.add(toUrn(grantedPrivilege)); // Add URN prefix back
      }
    }
  }

  return extendedPrivileges;
}

function getUserPrivileges(user) {
  var explicitPrivileges = _getExplicitPrivileges(user)
  var extendedPrivileges = _extendExplicitPrivileges(explicitPrivileges);
  var printablePrivileges = extendedPrivileges.stream().map(fromUrn).collect(Collectors.joining(", "));

  logger.debugv("User {0} has privileges: {1}", user.getUsername(), printablePrivileges);
  return extendedPrivileges;
}

function _evaluateForAdmin(identity) {
  if (identity.hasRealmRole('admin')) {
    logger.info("Granting all permissions to admin");
    return true;
  } else {
    logger.debugv("User {0} is not an admin, proceeding with privilege evaluation for normal user", identity.getId());
    return false;
  }
}

function _evaluateForUser(user, userId, privilege) {
  if (user === null || user === undefined) {
    logger.warnv("No user found for identity id: {0}", userId);
    return false;
  } else {
    var privileges = getUserPrivileges(user);
    var result = privileges.contains(privilege);
    var resultString = result ? "granted" : "denied";
    logger.infov("checking is user `{0}` has privilege `{1}`: {2}", user.getUsername(), fromUrn(privilege), resultString);

    return result;
  }
}

function evaluate(identity, user, privilege) {
  if (_evaluateForAdmin(identity)) {
    $evaluation.grant();
  } else if (_evaluateForUser(user, identity.getId(), privilege)) {
    $evaluation.grant();
  } else {
    $evaluation.denyIfNoEffect();
  }
}

// Authorization evaluation logic
var _context = $evaluation.getContext();

// Resource and scope information
var _permission = $evaluation.getPermission();
var _resource = _permission.getResource().getName();
var _scope = _permission.getScopes().toArray()[0].getName(); // cairo APIs request only one scope at max for any API endpoint
var privilege = _resource + ':' + _scope;

// Realm, Identity, and User information
var _authProvider = $evaluation.getAuthorizationProvider();
var _realm = _authProvider.getRealm();
var identity = _context.getIdentity();
var user = _authProvider.getKeycloakSession().users().getUserById(
  _realm,
  identity.getId()
);

logger.debug("---Starting evaluation---")
logger.debugv("Evaluating privilege: {0}", fromUrn(privilege));

evaluate(identity, user, privilege);

logger.debug("---Ending evaluation---");