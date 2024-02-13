package bio.ferlab.cqdg.ferload.clients

import org.apache.http.{HttpRequest, HttpRequestInterceptor}
import org.apache.http.protocol.HttpContext

class FerloadRequestInterceptor (token: String) extends HttpRequestInterceptor {

  def process(request: HttpRequest, context: HttpContext): Unit = {
    if (!request.containsHeader("Authorization")) {
      request.addHeader("Authorization", s"Bearer $token")
    }
  }
}
