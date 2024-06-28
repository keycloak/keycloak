var realm = $evaluation.getRealm();

if (realm.isUserInRealmRole('trinity', 'client-role-b')) {
    $evaluation.grant();
}
