package bio.ferlab.cqdg.ferload_save

import bio.ferlab.cqdg.etl.clients.FerloadClient
import bio.ferlab.cqdg.ferload_save.fhir.FhirClient.buildFhirClient
import bio.ferlab.cqdg.ferload_save.fhir.{AuthTokenInterceptor, FhirUtils}
import bio.ferlab.cqdg.ferload_save.keycloak.Auth
import bio.ferlab.cqdg.ferload_save.model.DrsObject
import bio.ferlab.cqdg.ferload_save.s3.S3Utils.buildS3Client
import ca.uhn.fhir.rest.client.api.IGenericClient
import org.hl7.fhir.r4.model.DocumentReference
import play.api.libs.json.Json
import software.amazon.awssdk.services.s3.S3Client

object FerloadImport extends App {

  val Array(study) = args

  withSystemExit {
    withLog {
      withConf { conf =>
        implicit val s3Client: S3Client = buildS3Client(conf.aws)
        implicit val fhirClient: IGenericClient = buildFhirClient(conf.fhir, conf.keycloak)
        val auth: Auth = new AuthTokenInterceptor(conf.keycloak).auth

        val token = auth.withToken { (_, rpt) => rpt }
        implicit val ferloadClient: FerloadClient = new FerloadClient(token)

        withReport(conf.aws.bucketName, "ferload") { _ => run() }
      }
    }
  }

  private def run()(implicit fhirClient: IGenericClient, ferloadClient: FerloadClient)  = {

    val studyVersion = FhirUtils.getStudyVersion(study)

    val documents = FhirUtils.requestExportFor("DocumentReference", studyId = study, studyVersion = studyVersion).asInstanceOf[List[DocumentReference]]

    documents.map(docRef => {

      val creationDate = docRef.getIdElement.getVersionIdPartAsLong match {
        case v if v == 1 => None
        case _ => FhirUtils.getCreationDate(docRef.getIdElement.getIdPart)
      }

      val drs = DrsObject.apply(docRef, creationDate)

      ferloadClient.saveObject(Json.toJson(drs).toString(), docRef.getIdElement.getIdPart)
    }).reduce(_ combine _)

  }
}