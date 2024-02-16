package bio.ferlab.cqdg.ferload.clients

import bio.ferlab.cqdg.ferload.conf.AWSConf
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.{CreateBucketRequest, PutObjectRequest}
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}

import java.io.File
import scala.jdk.CollectionConverters._
import java.net.URI

case class S3ClientTest (host: String, port: Int) {

  val AWS_ACCESS_KEY = "minioadmin"
  val AWS_SECRET_KEY = "minioadmin"
  val BUCKET_NAME = "cqdg-qa-file-import"



  implicit val s3: S3Client = buildS3Client(AWSConf(AWS_ACCESS_KEY, AWS_SECRET_KEY, s"http://$host:$port", bucketName = BUCKET_NAME, pathStyleAccess = true))
  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  def buildS3Client(conf: AWSConf): S3Client = {
    val confBuilder: S3Configuration = software.amazon.awssdk.services.s3.S3Configuration.builder()
      .pathStyleAccessEnabled(conf.pathStyleAccess)
      .build()
    val staticCredentialsProvider: StaticCredentialsProvider = StaticCredentialsProvider.create(
      AwsBasicCredentials.create(conf.accessKey, conf.secretKey)
    )
    val endpoint = URI.create(conf.endpoint)
    val s3: S3Client = S3Client.builder()
      .credentialsProvider(staticCredentialsProvider)
      .endpointOverride(endpoint)
      .region(Region.US_EAST_1)
      .serviceConfiguration(confBuilder)
      .httpClient(ApacheHttpClient.create())
      .build()
    s3
  }

//  s3://cqdg-qa-file-import/jmichaud/study1/dataset_data1/1001/S14018.cram
//  s3://cqdg-qa-file-import/jmichaud/study1/dataset_data2/1001/S14019.cram
  def init (): Unit = {
    createBuckets()

    val files = Map("data1" -> "S14018.cram","data2" -> "S14019.cram")

    files.foreach{ case(dataset: String, file: String) =>
      transferFromResource(s"jmichaud/study1/dataset_$dataset/1001", s"$file")
    }

  }

  private def createBuckets(): Unit = {
    val alreadyExistingBuckets = s3.listBuckets().buckets().asScala.collect { case b if b.name() == BUCKET_NAME => b.name() }
    val bucketsToCreate = Seq(BUCKET_NAME).diff(alreadyExistingBuckets)
    bucketsToCreate.foreach { b =>
      val buketRequest = CreateBucketRequest.builder().bucket(b).build()
      s3.createBucket(buketRequest)
    }
  }

  def transferFromResource(prefix: String, resource: String, bucket: String = BUCKET_NAME): Unit = {
    val f = new File(getClass.getResource(s"/s3files/$resource").toURI)
    val put = PutObjectRequest.builder().bucket(bucket).key(s"$prefix/${f.getName}").build()
    s3.putObject(put, RequestBody.fromFile(f))
  }
}