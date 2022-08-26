var realm = $evaluation.getRealm();
var attributes = realm.getUserAttributes('jdoe');

if (attributes.size() == 6 && attributes.containsKey('a1') && attributes.containsKey('a2') && attributes.get('a1').size() == 2 && attributes.get('a2').get(0).equals('3')) {
    $evaluation.grant();
}
