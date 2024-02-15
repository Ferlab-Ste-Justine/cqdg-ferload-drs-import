package bio.ferlab.cqdg.ferload.clients

import bio.ferlab.cqdg.ferload.fhir.FhirUtils.Constants.{DOCUMENT_FORMAT_CS, FULL_SIZE_SD}
import ca.uhn.fhir.context.{FhirContext, PerformanceOptionsEnum}
import ca.uhn.fhir.rest.client.api.{IGenericClient, ServerValidationModeEnum}
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent
import org.hl7.fhir.r4.model._

import scala.jdk.CollectionConverters._

object FhirClientTest{
  def init()(implicit fhirClient: IGenericClient): Unit = {

    createDocumentReferences()
    createStudy()

  }

  def buildClient(fhirHost: String, fhirPort: Int, token: String): IGenericClient = {
    val fhirBaseUrl = s"http://$fhirHost:$fhirPort/fhir"
    val fhirContext: FhirContext = FhirContext.forR4()
    fhirContext.setPerformanceOptions(PerformanceOptionsEnum.DEFERRED_MODEL_SCANNING)
    fhirContext.getRestfulClientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER)

    val hapiFhirInterceptor: AuthTokenInterceptorSpec = new AuthTokenInterceptorSpec(token)
    val client = fhirContext.newRestfulGenericClient(fhirBaseUrl)
    client.registerInterceptor(hapiFhirInterceptor)
    client
  }

  private def createDocumentReferences()(implicit fhirClient : IGenericClient): Unit = {
    val documentsNumbers = Seq("doc1","doc2")

    documentsNumbers.map(docNum => {
      val document = new DocumentReference()

      val studyCode = new Coding()
      val studyVersionCode = new Coding()
      studyCode.setCode("study:STUDY1")
      studyVersionCode.setCode("study_version:2")

      val meta = new Meta
      meta.addTag(studyCode).addTag(studyVersionCode)

      val a = new Attachment()
      val size = new Extension(FULL_SIZE_SD, new DecimalType(10))
      a.setUrl(s"s3://path_to_$docNum")
      a.setTitle(s"$docNum.cram")
      a.addExtension(size)

      val format = new Coding(DOCUMENT_FORMAT_CS, "CRAM", "CRAM")

      val content  = new DocumentReferenceContentComponent(a).setFormat(format)

      document.setContent(Seq(content).asJava)

      document.setMeta(meta)
      document.setId(s"DocumentReference/$docNum")
      fhirClient.update().resource(document).execute()
    })

  }

  private def createStudy()(implicit fhirClient : IGenericClient): Unit = {
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