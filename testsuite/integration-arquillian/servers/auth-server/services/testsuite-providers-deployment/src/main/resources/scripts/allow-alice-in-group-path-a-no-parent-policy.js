var realm = $evaluation.getRealm();

if (!realm.isUserInGroup('alice', '/Group A', false)) {
    $evaluation.grant();
}
