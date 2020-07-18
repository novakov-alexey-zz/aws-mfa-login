import ammonite.ops._
import ammonite.ops.ImplicitWd._
import scala.util.Try

val path = home / ".aws" / "credentials"

def overwriteFile(
    path: Path,
    accessKey: String,
    secretKey: String,
    sessionToken: String
): Unit = {
  write.over(
    path,
    s"""|[default]
        |aws_access_key_id = $accessKey
        |aws_secret_access_key = $secretKey
        |aws_session_token = $sessionToken""".stripMargin
  )
  println(s"Written to $path: ${(read.lines ! path).mkString("\n")}")
}

@main
def main(
    arnMfaDevice: String @doc(
      "arn of the MFA device like arn:aws:iam::123456789012:mfa/user"
    ),
    mfaToken: String @doc(
      "code from MFA token"
    )
) = {
  val getTokenCmd =
    s"aws sts get-session-token --serial-number $arnMfaDevice --token-code $mfaToken"
  val tokenOrError =
    Try(%%('bash, "-c", getTokenCmd)).toEither
      .map(_.out.lines.mkString)
      .left
      .map(_.getMessage())
  val res = for {
    json <- tokenOrError
    creds <-
      ujson
        .read(json)
        .obj
        .get("Credentials")
        .toRight("AWS Credentials are missing")
        .map(_.obj)
    access <- creds.get("AccessKeyId").toRight("AccessKeyId is missing")
    secret <- creds.get("SecretAccessKey").toRight("Secret is missing")
    session <- creds.get("SessionToken").toRight("Session is missing")
  } yield (access.str, secret.str, session.str)

  res.fold(println, { case (a, s, st) => overwriteFile(path, a, s, st) })
}
