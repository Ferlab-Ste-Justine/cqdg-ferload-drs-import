package bio.ferlab.cqdg.ferload.fhir

import bio.ferlab.cqdg.ferload.ClientType.ClientType
import bio.ferlab.cqdg.ferload.conf.KeycloakConf
import bio.ferlab.cqdg.ferload.keycloak.Auth
import ca.uhn.fhir.rest.client.api.{IClientInterceptor, IHttpRequest, IHttpResponse}
import org.slf4j.{Logger, LoggerFactory}

class AuthTokenInterceptor(conf: KeycloakConf, clientType: ClientType) extends IClientInterceptor {

  val LOGGER: Logger = LoggerFactory.getLogger(getClass)
  val auth = new Auth(conf, clientType)

  override def interceptRequest(theRequest: IHttpRequest): Unit = auth.withToken { (_, rpt) =>
    LOGGER.debug("HTTP request intercepted.  Adding Authorization header.")
    theRequest.addHeader("Authorization", s"Bearer $rpt")

  }

  override def interceptResponse(theResponse: IHttpResponse): Unit = {
    // Nothing to do here for now
  }
}
