package bio.ferlab.cqdg.ferload.models

import org.keycloak.representations.idm.authorization.ResourceRepresentation

import scala.jdk.CollectionConverters._

case class DrsObjectSpec(
                          id: String = "doc1",
                          name: Option[String] = Some("S14018.cram"),
                          size: Option[Long] = Some(35550724621L),
                          version: Option[String] = Some("2"),
                          mime_type: Option[String] = Some("CRAM"),
                          checksums: Option[List[Checksum]] = None,
                          description: Option[String] = None,
                          aliases: Option[List[String]] = None,
                          uris: List[String] = List("s3://cqdg-qa-file-import/jmichaud/study1/dataset_data2/1001/S14018.cram"),
                          scopes: Option[List[String]] = None
                    ){}

case class Checksum (
                      checksum: String,
                      `type`: String
                    )


object DrsObjectSpec{
  def apply(resource: ResourceRepresentation): DrsObjectSpec = {

    val restAttributes = resource.getAttributes

    new DrsObjectSpec(
      id = resource.getId,
      name = Option(resource.getDisplayName),
      size = Option(restAttributes.get("size")).flatMap(_.asScala.toList.headOption.map(_.toLong)),
      version = Option(restAttributes.get("version")).flatMap(_.asScala.toList.headOption),
      mime_type = Option(restAttributes.get("mime_type")).flatMap(_.asScala.toList.headOption),
      checksums = None, //FIXME TBD
      description = Option(restAttributes.get("description")).flatMap(_.asScala.toList.headOption),
      aliases = Option(restAttributes.get("aliases")).map(_.asScala.toList),
      uris = resource.getUris.asScala.toList,
      scopes = Option(restAttributes.get("scopes")).map(_.asScala.toList)
    )
  }
}