# Ammonite Script for CLI MFA credentials

Requirements:

- [Ammonite](https://ammonite.io/#Ammonite-REPL)

Usage:

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