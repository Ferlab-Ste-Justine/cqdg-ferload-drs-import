package bio.ferlab.cqdg.ferload.utils

import ca.uhn.fhir.rest.client.api.IGenericClient
import org.apache.commons.io.FileUtils
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsValue, Json}

import java.io.File
import java.net.URL
import java.time.ZoneId
import scala.collection.Seq
import scala.io.Source
import scala.util.{Failure, Success, Try}


object FhirTestUtils {
  val DEFAULT_ZONE_ID: ZoneId = ZoneId.of("UTC")
  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  def findById[A <: IBaseResource](id: String, resourceType: Class[A])(implicit fhirClient: IGenericClient): Option[A] = {
    Option(
      fhirClient.read()
        .resource(resourceType)
        .withId(id)
        .execute()
    )
  }

  def clearAll()(implicit fhirClient: IGenericClient): Unit = {
    val inParams = new Parameters()
    inParams
      .addParameter().setName("expungePreviousVersions").setValue(new BooleanType(true))
    inParams
      .addParameter().setName("expungeDeletedResources").setValue(new BooleanType(true))
    Seq("DocumentReference", "Organization", "Specimen", "Task", "ServiceRequest", "Person").foreach { r =>
      val t = fhirClient.delete()
        .resourceConditionalByUrl(s"$r?_lastUpdated=ge2017-01-01&_cascade=delete")
        .execute()

      println(s"Clean $r")
      fhirClient
        .operation()
        .onType(r)
        .named("$expunge")
        .withParameters(inParams)
        .execute()

    }


  }

  def init()(implicit fhirClient: IGenericClient): Unit = {
//    def downloadAndCreate(p: String) = {
////      val content = downloadIfNotInResources(p)
//
//      fhirClient.create().resource(content).execute()
//    }

    LOGGER.info("Init fhir container with extensions ...")

  }

  def parseJsonFromResource(resourceName: String): Try[JsValue] = {
    val source = Source.fromResource(resourceName)
    try {
      val strJson = source.mkString
      val parsedJson = Json.parse(strJson)
      Success(parsedJson)
    } catch {
      case e: Exception =>
        Failure(e)
    } finally {
      source.close()
    }
  }

  def getStringJsonFromResource(resourceName: String): Try[String] = {
    val source = Source.fromResource(resourceName)
    try {
      val strJson = source.mkString
      Success(strJson)
    } catch {
      case e: Exception =>
        Failure(e)
    } finally {
      source.close()
    }
  }
}


