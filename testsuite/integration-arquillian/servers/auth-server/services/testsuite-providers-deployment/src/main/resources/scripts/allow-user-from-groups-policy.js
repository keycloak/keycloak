var realm = $evaluation.getRealm();
var groups = realm.getUserGroups('jdoe');

if (groups.size() == 2 && groups.contains('/Group A/Group B') && groups.contains('/Group A/Group D')) {
    $evaluation.grant();
}
