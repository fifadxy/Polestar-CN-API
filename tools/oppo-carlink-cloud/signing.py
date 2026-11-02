"""Server-side CarLink signing contracts. Never embed appSecret/serverSecret in Android.

Based on OPPO docs 11112/11113 and the two user-supplied Java utilities.
No network calls, token acquisition, credential storage, or implicit retries.
"""
from __future__ import annotations
import hashlib
import hmac
import json
import math
from typing import Mapping
from urllib.parse import quote_plus

def _required(*values: str) -> None:
    if any(not isinstance(value, str) or not value for value in values):
        raise ValueError('Required signing field is empty')

def application_token_sign(app_key: str, server_secret: str, timestamp: int) -> str:
    _required(app_key, server_secret)
    if not isinstance(timestamp, int) or isinstance(timestamp, bool) or timestamp <= 0:
        raise ValueError('timestamp must be epoch milliseconds')
    return hashlib.sha256((app_key + server_secret + str(timestamp)).encode('utf-8')).hexdigest()

def _scalar(value: object) -> str:
    if value is None:
        return ''
    if isinstance(value, bool):
        return 'true' if value else 'false'
    if isinstance(value, (str, int)):
        return str(value)
    raise ValueError('Pass body as the exact serialized JSON string; unsupported parameter type')

def user_sign(parameters: Mapping[str, object], app_id: str, app_secret: str) -> str:
    _required(app_id, app_secret)
    # The supplied SignUtils.formatUrlMap concatenates key+value, without a colon.
    # This is deliberately distinct from the prose's name:value wording.
    fields = ''.join(key + _scalar(parameters[key]) for key in sorted(parameters)
                     if key.strip() and key not in ('sign', 'appId'))
    return hashlib.sha1((app_id + fields + app_secret).encode('utf-8')).hexdigest()

def application_sign(parameters: Mapping[str, object], timestamp: str, nonce: str,
                     app_key: str, server_secret: str) -> str:
    _required(timestamp, nonce, app_key, server_secret)
    if len({key.lower() for key in parameters}) != len(parameters):
        raise ValueError('Case-colliding parameter names are ambiguous')
    for value in parameters.values():
        if not isinstance(value, (str, int, float, bool, type(None))) or isinstance(value, float) and not math.isfinite(value):
            raise ValueError('Nested payloads must be serialized into their documented string field first')
    common = 'appkey:' + app_key + 'nonce:' + nonce + 'timestamp:' + timestamp
    payload = ''.join(key.lower() + '=' + json.dumps(parameters[key], ensure_ascii=False,
                     separators=(',', ':'), allow_nan=False) for key in sorted(parameters, key=str.lower))
    canonical = common + payload + server_secret
    # Java URLEncoder uses form encoding, including '+' for space and '%7E' for '~'.
    encoded = quote_plus(canonical, safe='*-._').replace('~', '%7E')
    return hashlib.sha256(encoded.encode('utf-8')).hexdigest()

def verify_signature(expected: str, received: str) -> bool:
    if not isinstance(expected, str) or not isinstance(received, str):
        return False
    try:
        return hmac.compare_digest(expected, received)
    except (TypeError, ValueError):
        return False

APP_TOKEN_LIFETIME_SECONDS = 24 * 60 * 60
APP_TOKEN_TRANSITION_SECONDS = 10 * 60
