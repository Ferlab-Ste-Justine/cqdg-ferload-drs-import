package bio.ferlab.cqdg.ferload_save.model

import bio.ferlab.cqdg.ferload_save.fhir.FhirUtils.Constants.{DOCUMENT_FORMAT_CS, FULL_SIZE_SD}
import bio.ferlab.cqdg.ferload_save.fhir.FhirUtils.versionRegex
import org.hl7.fhir.r4.model.{DecimalType, DocumentReference}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Writes}

import java.time.{LocalDateTime, ZoneId}
import scala.jdk.CollectionConverters._

case class DrsObject(
                      id: String,
                      name: Option[String],
                      size: Option[Long],
                      created_time: Option[LocalDateTime],
                      updated_time: Option[LocalDateTime],
                      version: Option[String],
                      mime_type: Option[String],
                      checksums: Option[List[Checksum]],
                      description: Option[String],
                      aliases: Option[List[String]],
                      uris: List[String],
                      scopes: Option[List[String]]
                    )

case class Checksum (
                      checksum: String,
                      `type`: String
                    )


object DrsObject{
  def apply(fhirDocument: DocumentReference, creationDate: Option[LocalDateTime] = None): DrsObject = {

    val documentContents = fhirDocument.getContent.asScala.toList

    val documentHistoryVersion = fhirDocument.getIdElement.getVersionIdPartAsLong

    val attachmentOpt = documentContents.map(c => c.getAttachment).headOption

    val formatOpt = documentContents.map(c => c.getFormat).find(f => f.getSystem == DOCUMENT_FORMAT_CS)

    val studyVersion = fhirDocument.getMeta.getTag.asScala.toList
      .flatMap(someCode => versionRegex.findFirstMatchIn(someCode.getCode).map(e => e.group(1))) match {
      case h::t => Some(h)
      case _ => None
    }

    val currentHistoryVersionDate = fhirDocument.getMeta.getLastUpdated.toInstant.atZone(ZoneId.systemDefault()).toLocalDateTime

    new DrsObject(
      id = fhirDocument.getIdElement.getIdPart,
      name = attachmentOpt.map(a => a.getTitle),
      size = attachmentOpt
        .map(a => a.getExtensionByUrl(FULL_SIZE_SD).getValue.asInstanceOf[DecimalType].getValue.longValue()),
      created_time = if(documentHistoryVersion == 1) Some(currentHistoryVersionDate) else creationDate,
      updated_time = if(documentHistoryVersion == 1) None else Some(currentHistoryVersionDate),
      version = studyVersion,
      mime_type = formatOpt.map(f => f.getDisplay),
      checksums = None, //FIXME TBD
      description = None, //FIXME TBD
      aliases = None, //FIXME TBD
      uris = attachmentOpt.map(a => a.getUrl).toList,
      scopes = None //FIXME TBD
    )
  }

  implicit val residentWrites: Writes[Checksum] = (
    (JsPath \ "checksum").write[String] and
      (JsPath \ "_type").write[String]
    )(r => (r.checksum, r.`type`))

  implicit val fileMetadataWrites: Writes[DrsObject] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "name").write[Option[String]] and
      (JsPath \ "size").write[Option[Long]] and
      (JsPath \ "created_time").write[Option[LocalDateTime]] and
      (JsPath \ "updated_time").write[Option[LocalDateTime]] and
      (JsPath \ "version").write[Option[String]] and
      (JsPath \ "mime_type").write[Option[String]] and
      (JsPath \ "checksum").write[Option[List[Checksum]]] and
      (JsPath \ "description").write[Option[String]] and
      (JsPath \ "aliases").write[Option[List[String]]] and
      (JsPath \ "uris").write[List[String]] and
      (JsPath \ "scopes").write[Option[List[String]]]
    )(p => (p.id, p.name, p.size, p.created_time, p.updated_time, p.version, p.mime_type,
    p.checksums, p.description, p.aliases, p.uris, p.scopes))
}