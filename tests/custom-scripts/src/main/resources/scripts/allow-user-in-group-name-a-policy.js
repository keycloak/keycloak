var realm = $evaluation.getRealm();

if (realm.isUserInGroup('marta', 'Group A')) {
    $evaluation.grant();
}
