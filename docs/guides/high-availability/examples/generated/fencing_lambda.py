from urllib.error import HTTPError

import boto3
import jmespath
import json
import os
import urllib3

from base64 import b64decode
from urllib.parse import unquote

# Prevent unverified HTTPS connection warning
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


class MissingEnvironmentVariable(Exception):
    pass


class MissingSiteUrl(Exception):
    pass


def env(name):
    if name in os.environ:
        return os.environ[name]
    raise MissingEnvironmentVariable(f"Environment Variable '{name}' must be set")


def handle_site_offline(labels):
    a_client = boto3.client('globalaccelerator', region_name='us-west-2')

    acceleratorDNS = labels['accelerator']
    accelerator = jmespath.search(f"Accelerators[?(DnsName=='{acceleratorDNS}'|| DualStackDnsName=='{acceleratorDNS}')]", a_client.list_accelerators())
    if not accelerator:
        print(f"Ignoring SiteOffline alert as accelerator with DnsName '{acceleratorDNS}' not found")
        return

    accelerator_arn = accelerator[0]['AcceleratorArn']
    listener_arn = a_client.list_listeners(AcceleratorArn=accelerator_arn)['Listeners'][0]['ListenerArn']

    endpoint_group = a_client.list_endpoint_groups(ListenerArn=listener_arn)['EndpointGroups'][0]
    endpoints = endpoint_group['EndpointDescriptions']

    # Only update accelerator endpoints if two entries exist
    if len(endpoints) > 1:
        # If the reporter endpoint is not healthy then do nothing for now
        # A Lambda will eventually be triggered by the other offline site for this reporter
        reporter = labels['reporter']
        reporter_endpoint = [e for e in endpoints if endpoint_belongs_to_site(e, reporter)][0]
        if reporter_endpoint['HealthState'] == 'UNHEALTHY':
            print(f"Ignoring SiteOffline alert as reporter '{reporter}' endpoint is marked UNHEALTHY")
            return

        offline_site = labels['site']
        endpoints = [e for e in endpoints if not endpoint_belongs_to_site(e, offline_site)]
        del reporter_endpoint['HealthState']
        a_client.update_endpoint_group(
            EndpointGroupArn=endpoint_group['EndpointGroupArn'],
            EndpointConfigurations=endpoints
        )
        print(f"Removed site={offline_site} from Accelerator EndpointGroup")

        take_infinispan_site_offline(reporter, offline_site)
        print(f"Backup site={offline_site} caches taken offline")
    else:
        print("Ignoring SiteOffline alert only one Endpoint defined in the EndpointGroup")


def endpoint_belongs_to_site(endpoint, site):
    lb_arn = endpoint['EndpointId']
    region = lb_arn.split(':')[3]
    client = boto3.client('elbv2', region_name=region)
    tags = client.describe_tags(ResourceArns=[lb_arn])['TagDescriptions'][0]['Tags']
    for tag in tags:
        if tag['Key'] == 'site':
            return tag['Value'] == site
    return false


def take_infinispan_site_offline(reporter, offlinesite):
    endpoints = json.loads(INFINISPAN_SITE_ENDPOINTS)
    if reporter not in endpoints:
        raise MissingSiteUrl(f"Missing URL for site '{reporter}' in 'INFINISPAN_SITE_ENDPOINTS' json")

    endpoint = endpoints[reporter]
    password = get_secret(INFINISPAN_USER_SECRET)
    url = f"https://{endpoint}/rest/v2/container/x-site/backups/{offlinesite}?action=take-offline"
    http = urllib3.PoolManager(cert_reqs='CERT_NONE')
    headers = urllib3.make_headers(basic_auth=f"{INFINISPAN_USER}:{password}")
    try:
        rsp = http.request("POST", url, headers=headers)
        if rsp.status >= 400:
            raise HTTPError(f"Unexpected response status '%d' when taking site offline", rsp.status)
        rsp.release_conn()
    except HTTPError as e:
        print(f"HTTP error encountered: {e}")


def get_secret(secret_name):
    session = boto3.session.Session()
    client = session.client(
        service_name='secretsmanager',
        region_name=SECRETS_REGION
    )
    return client.get_secret_value(SecretId=secret_name)['SecretString']


def decode_basic_auth_header(encoded_str):
    split = encoded_str.strip().split(' ')
    if len(split) == 2:
        if split[0].strip().lower() == 'basic':
            try:
                username, password = b64decode(split[1]).decode().split(':', 1)
            except:
                raise DecodeError
        else:
            raise DecodeError
    else:
        raise DecodeError

    return unquote(username), unquote(password)


def handler(event, context):
    print(json.dumps(event))

    authorization = event['headers'].get('authorization')
    if authorization is None:
        print("'Authorization' header missing from request")
        return {
            "statusCode": 401
        }

    expectedPass = get_secret(WEBHOOK_USER_SECRET)
    username, password = decode_basic_auth_header(authorization)
    if username != WEBHOOK_USER and password != expectedPass:
        print('Invalid username/password combination')
        return {
            "statusCode": 403
        }

    body = event.get('body')
    if body is None:
        raise Exception('Empty request body')

    body = json.loads(body)
    print(json.dumps(body))

    if body['status'] != 'firing':
        print("Ignoring alert as status is not 'firing', status was: '%s'" % body['status'])
        return {
            "statusCode": 204
        }

    for alert in body['alerts']:
        labels = alert['labels']
        if labels['alertname'] == 'SiteOffline':
            handle_site_offline(labels)

    return {
        "statusCode": 204
    }


INFINISPAN_USER = env('INFINISPAN_USER')
INFINISPAN_USER_SECRET = env('INFINISPAN_USER_SECRET')
INFINISPAN_SITE_ENDPOINTS = env('INFINISPAN_SITE_ENDPOINTS')
SECRETS_REGION = env('SECRETS_REGION')
WEBHOOK_USER = env('WEBHOOK_USER')
WEBHOOK_USER_SECRET = env('WEBHOOK_USER_SECRET')
