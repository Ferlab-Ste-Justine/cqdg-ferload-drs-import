package bio.ferlab.cqdg.etl.clients

import bio.ferlab.cqdg.ferload.ValidationResult
import bio.ferlab.cqdg.ferload.clients.BaseHttpClient
import bio.ferlab.cqdg.ferload.conf.KeycloakConf
import cats.data.Validated
import cats.data.Validated.Invalid
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

class FerloadClient(conf: KeycloakConf) extends BaseHttpClient(conf: KeycloakConf)  {


  val ferloadConfig: Config = ConfigFactory.load.getObject("ferload").toConfig
  val ferloadEndpoint: String = ferloadConfig.getString("url")

  def saveObject(payload: String, documentId: String): ValidationResult[String] = {
    val url = s"$ferloadEndpoint/v1/drs/ga4gh/object"
    val httpRequest = new HttpPost(url)
    httpRequest.setEntity(new StringEntity(payload))
    try {
      val (_, status) = executeHttpRequest(httpRequest)
      if (status < 300) {
        Validated.Valid(documentId)
      } else {
        Invalid(documentId).toValidatedNel
      }
    } catch {
      case e => throw e
    }


  }

}
