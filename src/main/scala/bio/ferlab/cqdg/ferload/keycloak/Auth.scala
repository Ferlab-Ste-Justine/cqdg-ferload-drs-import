package bio.ferlab.cqdg.ferload.keycloak

import bio.ferlab.cqdg.ferload.ClientType
import bio.ferlab.cqdg.ferload.ClientType.ClientType
import bio.ferlab.cqdg.ferload.conf.KeycloakConf
import org.keycloak.authorization.client.{AuthzClient, Configuration}
import org.keycloak.common.util.Time
import org.keycloak.representations.idm.authorization.AuthorizationRequest

import scala.jdk.CollectionConverters._

class Auth(conf: KeycloakConf, clientType: ClientType) {

  private val config = new Configuration()
  config.setRealm(conf.realm)
  config.setAuthServerUrl(conf.url)
  private val clientKey = clientType match {
    case ClientType.WriteResource => conf.resourceClientKey
    case ClientType.Read => conf.readClientKey
  }
  private val clientSecret = clientType match {
    case ClientType.WriteResource => conf.resourceClientSecret
    case ClientType.Read => conf.readClientSecret
  }
  config.setResource(clientKey)
  config.setCredentials(Map("secret" -> clientSecret).toMap[String, Object].asJava)
  private val authzClient = AuthzClient.create(config)

  private val req = new AuthorizationRequest()
  private var expiresAt = 0L
  private var rpt = ""
  private var accessToken = ""

  def withToken[T](f: (String, String) => T): T = {

    if (expiresAt == 0 || expiresAt < Time.currentTime()) {
      accessToken = authzClient.obtainAccessToken().getToken
      val resp = authzClient.authorization().authorize(req)
      val expiresIn = resp.getExpiresIn
      expiresAt = Time.currentTime() + expiresIn - 5
      rpt = resp.getToken
    }
    f(accessToken, rpt)
  }


}
