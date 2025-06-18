var contextAttributes = $evaluation.getContext().getAttributes();

if (contextAttributes.containsValue('kc.client.network.ip_address', '127.0.0.1') || contextAttributes.containsValue('kc.client.network.ip_address', '0:0:0:0:0:0:0:1')) {
    $evaluation.grant();
}
