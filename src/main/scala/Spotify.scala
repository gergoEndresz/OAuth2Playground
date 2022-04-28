import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpResponse, MediaRanges, RequestEntity, ResponseEntity, headers}
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpEncodings, OAuth2BearerToken}

import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import org.json._

  class SpotifyTokenAcquisitor(clientId: String,
                               clientSecret: String)
    extends TokenAcquisitor[OAuth2BearerToken](
      clientId,
      clientSecret,
      "https://accounts.spotify.com/api/token") {


    override val buildHeaders: scala.collection.immutable.Seq[HttpHeader] = {
      val authorization: HttpHeader = headers.Authorization(BasicHttpCredentials(clientId, clientSecret))
      scala.collection.immutable.Seq(authorization, accept, acceptEncoding)
    }

    override val buildEntity: RequestEntity = HttpEntity(ContentTypes.`application/x-www-form-urlencoded`, grantTypeString)

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


