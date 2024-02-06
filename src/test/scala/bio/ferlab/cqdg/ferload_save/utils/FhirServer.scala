package bio.ferlab.cqdg.ferload_save.utils

import bio.ferlab.cqdg.ferload_save.utils.containers.FhirServerContainer
import ca.uhn.fhir.context.{FhirContext, PerformanceOptionsEnum}
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.api.SummaryEnum
import ca.uhn.fhir.rest.client.api.{IGenericClient, ServerValidationModeEnum}
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.{Bundle, Coding, DocumentReference, IdType, Meta, ResearchStudy, Resource}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, TestSuite}
import org.slf4j.{Logger, LoggerFactory}

import scala.jdk.CollectionConverters._

trait FhirServer {
  val (fhirPort, isNew) = FhirServerContainer.startIfNotRunning()

  val fhirBaseUrl = s"http://localhost:$fhirPort/fhir"
  val fhirContext: FhirContext = FhirContext.forR4()
  fhirContext.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING)
  fhirContext.getRestfulClientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER)
  val parser: IParser = fhirContext.newJsonParser().setPrettyPrint(true)

  implicit val fhirClient: IGenericClient = fhirContext.newRestfulGenericClient(fhirBaseUrl)
  if(isNew) {
    FhirServerContainer.init()
  }

  createDocumentReferences()
  createStudy()

  private def createDocumentReferences(): Unit = {
    val documentsNumbers = Seq("doc1","doc2")

    documentsNumbers.map(docNum => {
      val document = new DocumentReference()

      val studyCode = new Coding()
      val studyVersionCode = new Coding()
      studyCode.setCode("study:STUDY1")
      studyVersionCode.setCode("study_version:2")

      val meta = new Meta
      meta.addTag(studyCode).addTag(studyVersionCode)

      document.setMeta(meta)
      document.setId(docNum)
      fhirClient.create().resource(document).execute()
    })

  }

  private def createStudy(): Unit = {
    val study = new ResearchStudy()

    val studyCode = new Coding()
    val studyVersionCode = new Coding()
    studyCode.setCode("study:STUDY1")
    studyVersionCode.setCode("study_version:2")

    val meta = new Meta
    meta.addTag(studyCode).addTag(studyVersionCode)

    study.setMeta(meta)
    study.setId("STUDY1")
    fhirClient.create().resource(study).execute()

  }

}

trait FhirServerSuite extends FhirServer with TestSuite with BeforeAndAfterAll with BeforeAndAfter {

  override def beforeAll(): Unit = {
    FhirTestUtils.clearAll()
  }

  def nextId(): String = java.util.UUID.randomUUID.toString

  def read[T <: IBaseResource](b: Bundle, theClass: Class[T]): Seq[T] = {

    b.getEntry.asScala.map { be =>

      val resourceId = id(be.getResource)
      val t: T = fhirClient.read().resource(theClass).withId(resourceId).execute()
      t
    }.toSeq
  }

  def id(r: Resource): String = {
    IdType.of(r).toUnqualifiedVersionless.toString
  }

  def searchFhir(resourceType: String, extraConditions: Option[String] = None): Bundle = {
    val url = extraConditions match {
      case Some(queryParams) => s"$resourceType?$queryParams"
      case None => s"$resourceType"
    }

    fhirClient.search().byUrl(url)
      .returnBundle(classOf[Bundle])
      .summaryMode(SummaryEnum.TRUE)
      .count(40)
      .execute()
  }

  def addElementToFhir(resource: Resource) = {
    fhirClient.create().resource(resource).execute()
  }
}

object StartFhirServer extends App with FhirServer {
  val LOGGER: Logger = LoggerFactory.getLogger(getClass)
  LOGGER.info("Fhir Server is started")
  while (true) {

  }
}