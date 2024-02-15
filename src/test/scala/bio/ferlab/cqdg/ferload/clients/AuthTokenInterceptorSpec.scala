package bio.ferlab.cqdg.ferload.clients

import ca.uhn.fhir.rest.client.api.{IClientInterceptor, IHttpRequest, IHttpResponse}

class AuthTokenInterceptorSpec(token: String) extends IClientInterceptor {

  override def interceptRequest(theRequest: IHttpRequest): Unit = {
    theRequest.addHeader("Authorization", s"Bearer $token")

  }

  override def interceptResponse(theResponse: IHttpResponse): Unit = {
    // Nothing to do here for now
  }
}
