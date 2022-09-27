var realm = $evaluation.getRealm();

if (realm.isUserInRealmRole('marta', 'role-b')) {
    $evaluation.grant();
}
