import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.{BodyPartEntity, ContentTypes, HttpEntity, HttpHeader, HttpResponse, Multipart, RequestEntity, ResponseEntity}
import akka.http.scaladsl.model.headers.{OAuth2BearerToken, `Content-Type`}
import akka.util.ByteString
import org.json.JSONObject

import scala.collection.immutable
import scala.concurrent.{ Future}
import scala.concurrent.duration.DurationInt

class OneTrust[OpenAuth](clientId: String,
                         clientSecret: String,
                         tokenEndpoint: String)
  extends TokenAcquisitor[OAuth2BearerToken](clientId, clientSecret, tokenEndpoint) {

  val bodyPartBoundary = "---011000010111000001101001"
  override def buildHeaders: immutable.Seq[HttpHeader] = {
    scala.collection.immutable.Seq(
      accept,
      acceptEncoding)
  }

  override def buildEntity: RequestEntity = {
    //HttpEntity(ContentTypes.`application/x-www-form-urlencoded`, grantTypeString)
//    val bodyParts: immutable.Seq[BodyPart] = collection.immutable.Seq(
//      Multipart.FormData.BodyPart("grant_type", Strict(ContentTypes.`text/plain(UTF-8)`, ByteString("client_credentials"))),
//      Multipart.FormData.BodyPart("client_id", Strict(ContentTypes.`text/plain(UTF-8)`, ByteString(clientId))),
//      Multipart.FormData.BodyPart("client_secret", Strict(ContentTypes.`text/plain(UTF-8)`, ByteString(clientSecret)))
//    )
    val multipartFormData = Multipart.FormData(
      Multipart.FormData.BodyPart("grant_type", Strict(ContentTypes.`text/plain(UTF-8)`, ByteString("client_credentials"))),
      Multipart.FormData.BodyPart("client_id", Strict(ContentTypes.`text/plain(UTF-8)`, ByteString(clientId))),
      Multipart.FormData.BodyPart("client_secret", Strict(ContentTypes.`text/plain(UTF-8)`, ByteString(clientSecret)))
    )
    print(f"[multipartFormData]: ${multipartFormData.parts.toString()}")
    multipartFormData.toEntity(bodyPartBoundary)
  }

  override def handleOK(entity: ResponseEntity): Future[OAuth2BearerToken] = {
    entity.toStrict(2 seconds)
      .map(r => {
        val responseAsString = (r.getData().utf8String)
        println(responseAsString)
        val nObject = new JSONObject(responseAsString)
        OAuth2BearerToken(nObject.get("access_token").toString)
      })
  }
}
