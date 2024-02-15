package bio.ferlab.cqdg.ferload.clients

import ca.uhn.fhir.context.{FhirContext, PerformanceOptionsEnum}
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.client.api.{IGenericClient, ServerValidationModeEnum}
import org.apache.commons.io.FileUtils
import org.hl7.fhir.r4.model._

import java.io.File
import java.nio.charset.StandardCharsets

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


  val ctx: FhirContext = FhirContext.forR4
  val parser: IParser = ctx.newJsonParser

  private def createDocumentReferences()(implicit fhirClient : IGenericClient): Unit = {
    for (i <- Seq(1,2)){
      val input = FileUtils.readFileToString(new File(s"src/test/resources/fhir/DocumentRef$i.json"), StandardCharsets.UTF_8)
      val doc1: DocumentReference = parser.parseResource(classOf[DocumentReference], input)
      doc1.setId(s"DocumentReference/doc$i")
      fhirClient.update().resource(doc1).execute()
    }
  }

  private def createStudy()(implicit fhirClient : IGenericClient): Unit = {
    val input = FileUtils.readFileToString(new File(s"src/test/resources/fhir/ResearchStudy.json"), StandardCharsets.UTF_8)
    val study: ResearchStudy = parser.parseResource(classOf[ResearchStudy], input)
    fhirClient.create().resource(study).execute()
  }



}