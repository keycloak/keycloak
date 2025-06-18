var contextAttributes = $evaluation.getContext().getAttributes();

if (contextAttributes.containsValue('kc.client.network.ip_address', '127.3.3.3') || contextAttributes.containsValue('kc.client.network.ip_address', '0:0:0:0:0:ffff:7f03:303')) {
    $evaluation.grant();
}
