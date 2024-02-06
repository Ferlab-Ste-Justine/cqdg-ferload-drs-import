package bio.ferlab.cqdg.ferload_save.fhir

import bio.ferlab.cqdg.ferload_save.conf.KeycloakConf
import bio.ferlab.cqdg.ferload_save.keycloak.Auth
import ca.uhn.fhir.rest.client.api.{IClientInterceptor, IHttpRequest, IHttpResponse}
import org.slf4j.{Logger, LoggerFactory}

class CustomHeaderInterceptor(name: String, value: String) extends IClientInterceptor {

  override def interceptRequest(theRequest: IHttpRequest): Unit =
    theRequest.addHeader(name, value)


  override def interceptResponse(theResponse: IHttpResponse): Unit = {
    // Nothing to do here for now
  }
}
