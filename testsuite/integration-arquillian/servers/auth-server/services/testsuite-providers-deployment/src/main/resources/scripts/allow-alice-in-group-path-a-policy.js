var realm = $evaluation.getRealm();

if (realm.isUserInGroup('alice', '/Group A')) {
    $evaluation.grant();
}
