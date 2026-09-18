var realm = $evaluation.getRealm();

if (realm.isUserInGroup('marta', 'Group C')) {
    $evaluation.grant();
}
