var realm = $evaluation.getRealm();

if (realm.isUserInClientRole('trinity', 'role-mapping-client', 'client-role-a')) {
    $evaluation.grant();
}
