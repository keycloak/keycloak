var realm = $evaluation.getRealm();
var roles = realm.getUserClientRoles('trinity', 'role-mapping-client');

if (roles.size() == 1 && roles.contains('client-role-a')) {
    $evaluation.grant();
}