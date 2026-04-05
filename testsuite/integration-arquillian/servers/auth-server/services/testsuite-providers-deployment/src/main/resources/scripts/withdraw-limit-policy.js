var context = $evaluation.getContext();
var attributes = context.getAttributes();
var withdrawValue = attributes.getValue('my.bank.account.withdraw.value');

if (withdrawValue && withdrawValue.asDouble(0) <= 100) {
    $evaluation.grant();
}
