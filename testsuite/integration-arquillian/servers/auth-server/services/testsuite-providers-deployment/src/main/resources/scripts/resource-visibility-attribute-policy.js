var createPermission = $evaluation.getPermission();
var resource = createPermission.getResource();

if (resource) {
    var attributes = resource.getAttributes();
    var visibility = attributes.get('visibility');

    if (visibility && "private".equals(visibility.get(0))) {
        $evaluation.deny();
    } else {
        $evaluation.grant();
    }
}