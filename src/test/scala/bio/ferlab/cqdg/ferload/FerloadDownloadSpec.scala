package bio.ferlab.cqdg.ferload

import bio.ferlab.cqdg.etl.clients.FerloadClient
import bio.ferlab.cqdg.ferload.clients.{FerloadClientTest, KeycloakClientTest, S3ClientTest}
import bio.ferlab.cqdg.ferload.conf.AWSConf
import bio.ferlab.cqdg.ferload.models.DrsObjectSpec
import bio.ferlab.cqdg.ferload.s3.S3Utils
import bio.ferlab.cqdg.ferload.utils.Utils.{fetchToken, fetchUserToken}
import bio.ferlab.cqdg.ferload.utils._
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

import java.io.File

class FerloadDownloadSpec extends FlatSpec with Matchers with BeforeAndAfterEach with TestContainerForAll {

  override val containerDef =
    DockerComposeContainer.Def(
      new File("src/test/resources/docker-compose-download.yml"),
      tailChildContainers = true,
      exposedServices = Seq(
        ExposedService("auth_1", AUTH_PORT), //todo implement wait strategy
        ExposedService("minio_1", MINIO_PORT), //todo implement wait strategy
        ExposedService("ferload_1", FERLOAD_PORT), //todo implement wait strategy
      )
    )

  val STUDY = "study1"
  val FILE_BUCKET = "cqdg-qa-file-import"

  val testFiles = Seq(
    DrsObjectSpec(
      id = "Fie1",
      name = Some("S14018.cram"),
      uris = List(s"s3://$FILE_BUCKET/jmichaud/$STUDY/dataset_data1/1001/S14018.cram"),
      scopes = Some(List(STUDY, "data1"))
    ),
      DrsObjectSpec(
      id = "Fie2",
      name = Some("S14019.cram"),
      uris = List(s"s3://$FILE_BUCKET/jmichaud/$STUDY/dataset_data2/1001/S14019.cram"),
      scopes = Some(List(STUDY, "data2"))
    )
  )


  //TODO implement containers creating and provisioning in Before and stop in After
  "run" should "return no errors" in {
    withContainers { composedContainers =>

      //Ambassador container exposed ports
      val authPort = composedContainers.getServicePort("auth_1", AUTH_PORT)
      val minioPort = composedContainers.getServicePort("minio_1", MINIO_PORT)
      val ferloadPort = composedContainers.getServicePort("ferload_1", FERLOAD_PORT)

      // Create and Provision Keycloak
      val keycloakClientTest = KeycloakClientTest("localhost", authPort)
      keycloakClientTest.initDownload(RESOURCE_CLIENT, FHIR_CLIENT, testFiles)

      // Create and Provision S3
      val s3ClientTest = S3ClientTest("localhost", minioPort)
      s3ClientTest.init()

      // Ferload
//      val tokenFerload = fetchToken(composedContainers.container.getContainerByServiceName("ferload_1").get(), RESOURCE_CLIENT, RESOURCE_CLIENT_SECRET)
      val tokenUserFerload = fetchUserToken(composedContainers.container.getContainerByServiceName("ferload_1").get(), RESOURCE_CLIENT, RESOURCE_CLIENT_SECRET)
      implicit val ferloadClient: FerloadClient = new FerloadClientTest(tokenUserFerload, "localhost", ferloadPort)

      val filesList = Seq("S14018.cram", "S14019.cram")

      FerloadDownload.runNew(FILE_BUCKET, filesList)

      1 shouldBe(1)

    }
  }
}
