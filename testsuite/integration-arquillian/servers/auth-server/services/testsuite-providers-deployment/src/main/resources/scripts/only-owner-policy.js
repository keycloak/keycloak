var permission = $evaluation.getPermission();
var identity = $evaluation.getContext().getIdentity();
var resource = permission.getResource();

if (resource) {
    if (resource.getOwner().equals(identity.getId())) {
        $evaluation.grant();
    }
}