var realm = $evaluation.getRealm();

if (realm.isUserInGroup('alice', '/Group A/Group B/Group E')) {
    $evaluation.grant();
}
