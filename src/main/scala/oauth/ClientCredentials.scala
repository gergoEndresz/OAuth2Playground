package oauth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpEncodings, OAuth2BearerToken}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.json.JSONObject

import scala.collection.immutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

  object tokenProviders {
    def OneTrustTokenProvider(clientId: String,
                               clientSecret: String,
                               tokenEndpoint: String): OneTrustTokenProvider = {
      new OneTrustTokenProvider(clientId, clientSecret, tokenEndpoint)
    }

    def SpotifyTokenProvider(clientId: String,
                             clientSecret: String): SpotifyTokenProvider ={
      new SpotifyTokenProvider(clientId, clientSecret)
    }
  }
  abstract class ClientCredentialsProvider[TokenWrapper](val clientId: String,
                                                         val clientSecret: String,
                                                         val tokenEndpoint: String,
                                                         val method: HttpMethod = HttpMethods.POST,
                                                         val grantTypeString: String = "grant_type=client_credentials",
                                                         val accept: HttpHeader = headers.Accept(MediaRanges.`*/*`),
                                                         val acceptEncoding: HttpHeader = headers.`Accept-Encoding`(HttpEncodings.deflate)) {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    def buildHeaders: scala.collection.immutable.Seq[HttpHeader]
    def buildEntity: RequestEntity
    def handleOK(entity: ResponseEntity): Future[TokenWrapper]

    def handleBad(httpResponse: HttpResponse): Any = {
      println("Something went wrong, investigate!")
      println(f"Status: ${httpResponse.status.value}")
      println(f"Response header: ${httpResponse.headers.toString()}")
      println(f"Response body:${httpResponse.entity.toString}")
      println(f"Protocol:${httpResponse.protocol}")

      httpResponse.entity.discardBytes()
      ()
    }

    def buildRequest(): HttpRequest = {
      HttpRequest(
        method = method,
        uri = tokenEndpoint,
        entity = buildEntity,
        headers = buildHeaders
      )
    }

    def sendRequest(request: HttpRequest): Future[HttpResponse] = {
      println(request.toString())
      println(f"[headers]: ${request.entity.httpEntity.toString}")
      println(f"[headers]: ${request.headers.toString}")
      println(f"[entity]: ${request.entity.toString()}")

      Http().singleRequest(request)
    }

    def acquireToken(): Future[Any] = {
      val responseFuture = sendRequest(buildRequest())
      val future = processResponse(responseFuture)
      future
    }

    def handleStatuses(httpResponse: HttpResponse): Any = httpResponse match {
      case HttpResponse(StatusCodes.OK, _, entity, _) => handleOK(entity)
      case HttpResponse(_, headers, entity, _) => handleBad(httpResponse)
    }

    def processResponse(response: Future[HttpResponse]): Future[Any] = {
      response.map(handleStatuses)
    }
}

final case class OneTrustTokenProvider(override val clientId: String,
                                       override val clientSecret: String,
                                       override val tokenEndpoint: String)
  extends ClientCredentialsProvider[OAuth2BearerToken](clientId, clientSecret, tokenEndpoint) {

  val bodyPartBoundary = "---011000010111000001101001"
  override def buildHeaders: immutable.Seq[HttpHeader] = {
    scala.collection.immutable.Seq(
      accept,
      acceptEncoding,
      //oauth.CustomContentTypeHeader(oauth.CustomContentTypeHeader.defaultValue)
      //RawHeader("content-type", "multipart/form-data; boundary=---011000010111000001101001")
    )
  }

  override def buildEntity: RequestEntity = {
    //IndefiniteLength("x")
    // Strict(ContentTypes.`text/plain(UTF-8)`, ByteString("client_credentials")).toStrict
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
        println(nObject.get("access_token"))
        OAuth2BearerToken(nObject.get("access_token").toString)
      })
  }
}

final case class SpotifyTokenProvider(override val clientId: String,
                                      override val clientSecret: String,
                                      override val tokenEndpoint: String = "https://accounts.spotify.com/api/token")
  extends ClientCredentialsProvider[OAuth2BearerToken] (clientId, clientSecret, tokenEndpoint){

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

