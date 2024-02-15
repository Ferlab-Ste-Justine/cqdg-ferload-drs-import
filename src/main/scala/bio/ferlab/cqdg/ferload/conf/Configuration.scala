package bio.ferlab.cqdg.ferload.conf

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.generic.auto._


case class AWSConf(
                    accessKey: String,
                    secretKey: String,
                    endpoint: String,
                    bucketName: String,
                    pathStyleAccess: Boolean,
                  )

case class FhirConf(url: String)

case class KeycloakConf(
                         realm: String,
                         url: String,
                         readClientKey: String,
                         readClientSecret: String,
                         resourceClientKey: String,
                         resourceClientSecret: String,
                       )

case class FerloadConf(url: String)

case class Conf(aws: AWSConf, fhir: FhirConf, keycloak: KeycloakConf, ferload: FerloadConf)

object Conf {

  def readConf(): ValidatedNel[String, Conf] = {
    val confResult: Result[Conf] = ConfigSource.default.load[Conf]
    confResult match {
      case Left(errors) =>
        val message = errors.prettyPrint()
        message.invalidNel[Conf]
      case Right(conf) => conf.validNel[String]
    }
  }
}
