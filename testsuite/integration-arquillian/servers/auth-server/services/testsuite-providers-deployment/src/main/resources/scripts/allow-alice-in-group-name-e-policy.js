var realm = $evaluation.getRealm();

if (realm.isUserInGroup('alice', 'Group E')) {
    $evaluation.grant();
}
