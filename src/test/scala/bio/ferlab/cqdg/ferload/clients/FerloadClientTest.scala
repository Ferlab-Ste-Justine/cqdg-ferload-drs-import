package bio.ferlab.cqdg.ferload.clients

import bio.ferlab.cqdg.etl.clients.FerloadClient
import bio.ferlab.cqdg.ferload.ValidationResult
import cats.data.Validated
import cats.data.Validated.Invalid
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

class FerloadClientTest(token: String, host: String, port: Int) extends FerloadClient (token) {


  override val ferloadEndpoint: String = s"http://$host:$port"
}
