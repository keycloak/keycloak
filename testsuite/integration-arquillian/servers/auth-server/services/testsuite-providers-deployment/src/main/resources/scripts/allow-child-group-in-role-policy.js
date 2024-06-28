var realm = $evaluation.getRealm();

if (realm.isGroupInRole('/Group A/Group D', 'role-b')) {
    $evaluation.grant();
}

