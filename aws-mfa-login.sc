import ammonite.ops._
import ammonite.ops.ImplicitWd._
import scala.util.Try
import scala.sys.process._

val credentialsPath = home / ".aws" / "credentials"
val securityKeysPath = home / ".aws" / "keys"

def writeCredentials(
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

def getToken(arnMfaDevice: String, mfaToken: String) = {
  val getTokenCmd =
    s"aws sts get-session-token --serial-number $arnMfaDevice --token-code $mfaToken"

  Try(%%('bash, "-c", getTokenCmd)).toEither
    .map(_.out.lines.mkString)
    .left
    .map(_.getMessage())
}

@main
def main(
    arnMfaDevice: String @arg(
      doc = "arn of the MFA device like arn:aws:iam::123456789012:mfa/your_user"
    ),
    mfaToken: String @arg(doc = "code from MFA token")
) = {
  val res = for {
    _ <- Try(s"rm $credentialsPath".!).toEither.left.map(t =>
      s"Eror upon old file deletion at $credentialsPath, reason: ${t.getMessage}"
    )
    keys <- Try(
      os.read(home / ".aws" / "keys").linesIterator.toList
    ).toEither.left
      .map(t =>
        s"Failed to read file with security keys at $securityKeysPath: ${t.getMessage}"
      )
      .flatMap { keys =>
        keys match {
          case accessKey :: secretKey :: Nil => Right(accessKey -> secretKey)
          case _ =>
            Left(
              s"Please make sure there are 2 lines at $securityKeysPath file for aws_access_key_id and aws_secret_access_key values correspondingly"
            )
        }
      }
    _ <- Try(
      s"aws configure set aws_access_key_id ${keys._1} --profile default".!
    ).toEither.left.map(t => s"Failed to set access_key_id: ${t.getMessage}")
    _ <- Try(
      s"aws configure set aws_secret_access_key ${keys._2} --profile default".!
    ).toEither.left.map(t =>
      s"Failed to set aws_secret_access_key: ${t.getMessage}"
    )
    json <- getToken(arnMfaDevice, mfaToken)
    creds <-
      ujson
        .read(json)
        .obj
        .get("Credentials")
        .toRight("AWS Credentials are missing")
        .map(_.obj)
    access <- creds.get("AccessKeyId").toRight("AccessKeyId is missing")
    secret <- creds.get("SecretAccessKey").toRight("SecretAccessKey is missing")
    session <- creds.get("SessionToken").toRight("SessionToken is missing")
  } yield (access.str, secret.str, session.str)

  res.fold(
    println,
    { case (a, s, st) => writeCredentials(credentialsPath, a, s, st) }
  )
}
