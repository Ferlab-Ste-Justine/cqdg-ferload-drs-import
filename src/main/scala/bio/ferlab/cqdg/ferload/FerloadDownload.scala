package bio.ferlab.cqdg.ferload

import bio.ferlab.cqdg.etl.clients.FerloadClient
import bio.ferlab.cqdg.ferload.fhir.FhirClient.buildFhirClient
import bio.ferlab.cqdg.ferload.fhir.{AuthTokenInterceptor, FhirUtils}
import bio.ferlab.cqdg.ferload.keycloak.Auth
import bio.ferlab.cqdg.ferload.model.DrsObject
import bio.ferlab.cqdg.ferload.s3.S3Utils
import bio.ferlab.cqdg.ferload.s3.S3Utils.{buildS3Client, buildS3PreSigned}
import ca.uhn.fhir.rest.client.api.IGenericClient
import org.hl7.fhir.r4.model.DocumentReference
import play.api.libs.json.Json
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

object FerloadDownload extends App {

  val Array(study) = args

  withSystemExit {
    withLog {
      withConf { conf =>
        implicit val s3Presigner: S3Presigner = buildS3PreSigned(conf.aws)
        implicit val s3Client: S3Client = buildS3Client(conf.aws)

        implicit val fhirClient: IGenericClient = buildFhirClient(conf.fhir, conf.keycloak)

        val filesBucket = conf.aws.bucketName

        val authResource: Auth = new AuthTokenInterceptor(conf.keycloak, ClientType.WriteResource).auth
        val token = authResource.withToken { (_, rpt) => rpt }
        implicit val ferloadClient: FerloadClient = new FerloadClient(token)

        withReport(conf.aws.bucketName, "ferload") { _ => run(study) }
      }
    }
  }

  def run(study: String)(implicit fhirClient: IGenericClient, ferloadClient: FerloadClient)  = {

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

  def runNew(bucket: String, fileList: Seq[String])(implicit ferloadClient: FerloadClient)  = {

    val response = ferloadClient.getObjects(fileList.toString(), "th") //fixme

//    val url = S3Utils.generatePreSignedUrl(bucket, prefix)

    println("TOTOTOTO")
  }
}