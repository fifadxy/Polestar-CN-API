import hashlib
import unittest
from signing import application_token_sign, application_sign, user_sign, verify_signature

class Signatures(unittest.TestCase):
    def test_token_uses_server_secret_and_milliseconds(self):
        expected=hashlib.sha256(b'test-keytest-server1700000000000').hexdigest()
        self.assertEqual(expected,application_token_sign('test-key','test-server',1700000000000))
        with self.assertRaises(ValueError):application_token_sign('key','',1700000000000)
    def test_user_signature_matches_supplied_java_helper_and_exact_body(self):
        params={'nonce':'xyz','body':'{"x": 1}','appId':'ignored','sign':'ignored','timestamp':123}
        expected=hashlib.sha1(b'appbody{"x": 1}noncexyztimestamp123secret').hexdigest()
        self.assertEqual(expected,user_sign(params,'app','secret'))
        self.assertNotEqual(expected,user_sign({**params,'body':'{"x":1}'},'app','secret'))
    def test_application_uses_java_form_encoding_and_sorted_lowercase_fields(self):
        canonical=b'appkey%3Ak+eynonce%3An%7Etimestamp%3A123a%3Dtruez%3D%22%E4%B8%AD+%2B%22secret'
        self.assertEqual(hashlib.sha256(canonical).hexdigest(),application_sign({'Z':'中 +','a':True},'123','n~','k ey','secret'))
    def test_ambiguous_names_and_nested_payloads_fail_closed(self):
        for values in ({'A':1,'a':2},{'body':{'a':1}}):
            with self.assertRaises(ValueError):application_sign(values,'123','nonce','key','secret')
    def test_comparison_is_exact(self):
        self.assertTrue(verify_signature('abc','abc'));self.assertFalse(verify_signature('abc','ABC'));self.assertFalse(verify_signature('abc','中文'))

if __name__=='__main__':unittest.main()
