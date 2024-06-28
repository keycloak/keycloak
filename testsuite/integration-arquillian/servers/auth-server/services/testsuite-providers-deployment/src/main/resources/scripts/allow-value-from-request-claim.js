var context = $evaluation.getContext();
var attributes = context.getAttributes();
var claim = attributes.getValue('request-claim');

if (claim && claim.asString(0) == 'expected-value') {
    $evaluation.grant();
}