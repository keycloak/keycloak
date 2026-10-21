var permission = $evaluation.getPermission();
var resource = permission.getResource();
var attributes = resource.getAttributes();

if (attributes.size() == 2 && attributes.containsKey('a1') && attributes.containsKey('a2') && attributes.get('a1').size() == 2 && attributes.get('a2').get(0).equals('3') && resource.getAttribute('a1').size() == 2 && resource.getSingleAttribute('a2').equals('3')) {
    $evaluation.grant();
}


