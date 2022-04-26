var context = $evaluation.getContext();
var attributes = context.getAttributes();
var withdrawalAmount = attributes.getValue('withdrawal.amount');

if (withdrawalAmount && withdrawalAmount.asDouble(0) <= 100) {
    $evaluation.grant();
}