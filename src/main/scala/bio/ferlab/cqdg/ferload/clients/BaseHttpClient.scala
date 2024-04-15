package bio.ferlab.cqdg.ferload.clients

import bio.ferlab.cqdg.ferload.conf.KeycloakConf
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.http.util.EntityUtils

class BaseHttpClient(conf: KeycloakConf) {

  val httpBuilder: HttpClientBuilder =
    HttpClientBuilder.create()
    .addInterceptorFirst(new FerloadRequestInterceptor(conf))

  val http: CloseableHttpClient = httpBuilder.build()
  val charsetUTF8 = "UTF-8"
  sys.addShutdownHook(http.close())

  def executeHttpRequest(request: HttpRequestBase): (Option[String], Int) = {
    val response: HttpResponse = http.execute(request)
    val body = Option(response.getEntity).map(e => EntityUtils.toString(e, charsetUTF8))
    // always properly close
    EntityUtils.consumeQuietly(response.getEntity)
    (body, response.getStatusLine.getStatusCode)
  }

}
