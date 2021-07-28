# Ammonite Script for CLI MFA credentials

Requirements:

- [Ammonite](https://ammonite.io/#Ammonite-REPL)

Usage:

Put your `aws_access_key_id` and `aws_secret_access_key` to a file at ~/.aws/keys like below:

~/.aws/keys:

```bash
<my_aws_access_key_id>
<my_aws_secret_access_key>
```

```bash
amm aws-mfa-login.sc <arn-mfa-device> <mfa-token>
```

result:

```bash
cat ~/.aws/credentials

[default]
aws_access_key_id = <your access key stored here>
aws_secret_access_key = <your secret access key stored here>
aws_session_token = <your session token stored here>
```