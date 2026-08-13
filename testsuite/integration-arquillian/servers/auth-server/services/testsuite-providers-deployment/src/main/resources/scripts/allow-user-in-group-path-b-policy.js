var realm = $evaluation.getRealm();

if (realm.isUserInGroup('marta', '/Group A/Group B')) {
    $evaluation.grant();
}
