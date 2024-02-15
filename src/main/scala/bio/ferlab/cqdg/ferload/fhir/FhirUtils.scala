package bio.ferlab.cqdg.ferload.fhir

import ca.uhn.fhir.rest.client.api.IGenericClient
import org.apache.http.HttpHost
import org.apache.http.client.utils.URIUtils
import org.hl7.fhir.r4.model._

import java.net.URI
import java.time.{LocalDateTime, ZoneId}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
import scala.language.reflectiveCalls


object FhirUtils {

  val baseFhirServer = "https://fhir.cqdg.ca"
  val versionRegex = "^study_version:(.+)$".r

  object Constants {
    val FULL_SIZE_SD = s"$baseFhirServer/StructureDefinition/FullSizeExtension"
    val DOCUMENT_FORMAT_CS = s"$baseFhirServer/CodeSystem/document-format"

  }

  private def getResourcesFromBundle(bundle: Bundle): mutable.Buffer[DomainResource] = {
    bundle.getEntry
      .asScala
      .map(entry => entry.getResource.asInstanceOf[DomainResource])
  }

  private def replaceBaseUrl(url: String, replaceHost: String) = {
    val pattern = """(^http[s]?:\/\/.[^\/]+)\/?""".r
    val cleanUrl = pattern.findAllIn(replaceHost).group(1)
    URIUtils.rewriteURI(URI.create(url), HttpHost.create(cleanUrl)).toString
  }

  def requestExportFor(`type`: String, studyId: String, studyVersion: String)(implicit fhirClient: IGenericClient): List[DomainResource] = {
    val resources: ListBuffer[DomainResource] = new ListBuffer[DomainResource]()

    val bundle = fhirClient.search()
      .forResource(`type`)
      .returnBundle(classOf[Bundle])

    val bundleWithStudy = bundle.withTag(null, s"study:$studyId").withTag(null, s"study_version:$studyVersion")

    var query = bundleWithStudy.execute()
    resources.addAll(getResourcesFromBundle(query))

    while (query.getLink("next") != null) {
      query.getLink("next").setUrl(replaceBaseUrl(query.getLink("next").getUrl, fhirClient.getServerBase))
      query = fhirClient.loadPage().next(query).execute()
      resources.addAll(getResourcesFromBundle(query))
    }
    resources.toList
  }

  def getCreationDate(documentId: String)(implicit fhirClient: IGenericClient): Option[LocalDateTime] = {

    try {
      val firstDocument = fhirClient.read()
        .resource(classOf[DocumentReference])
        .withUrl(s"DocumentReference/$documentId/_history/1")
        .execute()
      Some(firstDocument.getMeta.getLastUpdated.toInstant.atZone(ZoneId.systemDefault()).toLocalDateTime)
    } catch {
      case e: Exception => None
    }
  }

  def getStudyVersion(studyId: String)(implicit fhirClient: IGenericClient): String = {
    try {
      val study = fhirClient.search()
        .byUrl(s"ResearchStudy?_tag=study:$studyId")
        .returnBundle(classOf[Bundle])
        .execute()

      val tags: List[Coding] = study.getEntry.asScala.toList.headOption
        .getOrElse(throw new RuntimeException(s"Failed to retrieve study for $studyId"))
        .getResource.getMeta.getTag.asScala.toList

      val studyVersionOpt = tags
        .find(t => versionRegex.matches(t.getCode))
        .flatMap(e => versionRegex.findFirstMatchIn(e.getCode).map(m  => m.group(1)))

      studyVersionOpt.getOrElse(throw new RuntimeException(s"Failed to retrieve version for $studyId"))
    } catch {
      case e: Exception => throw e
    }
  }

}
