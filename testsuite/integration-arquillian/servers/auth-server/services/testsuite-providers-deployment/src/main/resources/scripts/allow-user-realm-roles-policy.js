var realm = $evaluation.getRealm();
var roles = realm.getUserRealmRoles('marta');

if (roles.size() == 2 && roles.contains('uma_authorization') && roles.contains('role-a')) {
    $evaluation.grant();
}

