package bio.ferlab.cqdg.ferload.clients

import bio.ferlab.cqdg.ferload.conf.KeycloakConf
import bio.ferlab.cqdg.ferload.keycloak.Auth
import org.apache.http.{HttpRequest, HttpRequestInterceptor}
import org.apache.http.protocol.HttpContext
import org.slf4j.{Logger, LoggerFactory}

class FerloadRequestInterceptor (conf: KeycloakConf) extends HttpRequestInterceptor {

  val LOGGER: Logger = LoggerFactory.getLogger(getClass)
  val auth = new Auth(conf)

  override def process(request: HttpRequest, context: HttpContext): Unit = auth.withToken { (token, rpt) =>
    LOGGER.debug("HTTP request intercepted.  Adding Authorization header.")
    request.addHeader("Authorization", s"Bearer $token") //FIXME

  }
}
