import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, HttpHeader, HttpMethod, HttpMethods, HttpRequest, HttpResponse, MediaRanges, RequestEntity, StatusCode, StatusCodes, headers}
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, HttpEncodings, OAuth2BearerToken, `Content-Type`}
import akka.stream.ActorMaterializer

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import org.json._

import scala.util.Try

object Spotify extends App {

  abstract class TokenAcquisitor[TokenWrapper](val clientId: String,
                                               val clientSecret: String,
                                               val toklenEndpoint: String,
                                               val method: HttpMethod = HttpMethods.POST) {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = system.dispatcher

    def buildHeaders: scala.collection.immutable.Seq[HttpHeader]
    def buildBody: RequestEntity

    def buildRequest(): HttpRequest = {
      HttpRequest(
        method = method,
        uri = tokenEndpoint,
        entity = buildBody,
        headers = buildHeaders
      )
    }

    def sendRequest(request: HttpRequest): Future[HttpResponse] = {
      Http().singleRequest(request)
    }

    def acquireToken(): Future[Any] = {
      processResponse(sendRequest(buildRequest()))
    }
    def handleOK(httpResponse: HttpResponse): Future[TokenWrapper]
    def handleNonOk(httpResponse: HttpResponse): Any = println("Something went wrong, investigate!")

    def handleStatuses(httpResponse: HttpResponse): Any = httpResponse.status match {
      case StatusCodes.OK => handleOK(httpResponse)
      case _ => handleNonOk(httpResponse)
    }

    def processResponse(response: Future[HttpResponse]): Future[Any] = {
      // todo
      // todo what if it is a Failure
      response.map(handleStatuses)
    }

  }

  class SpotifyTokenAcquisitor(clientId: String,
                               clientSecret: String,
                               toklenEndpoint: String,
                               val grantTypeString: String = "grant_type=client_credentials",
                               val accept: HttpHeader = headers.Accept(MediaRanges.`*/*`),
                               val acceptEncoding: HttpHeader = headers.`Accept-Encoding`(HttpEncodings.deflate))
    extends TokenAcquisitor[OAuth2BearerToken](clientId, clientSecret, toklenEndpoint) {


    override val buildHeaders: scala.collection.immutable.Seq[HttpHeader] = {
      val authorization: HttpHeader = headers.Authorization(BasicHttpCredentials(clientId, clientSecret))
      scala.collection.immutable.Seq(authorization, accept, acceptEncoding)
    }

    override val buildBody: RequestEntity = HttpEntity(ContentTypes.`application/x-www-form-urlencoded`, grantTypeString)



    override def handleOK(httpResponse: HttpResponse): Future[OAuth2BearerToken] = {
      httpResponse.entity.toStrict(2 seconds)
        .map(r => {
          val responseAsString = (r.getData().utf8String)
          println(responseAsString)
          val nObject = new JSONObject(responseAsString)
          OAuth2BearerToken(nObject.get("access_token").toString)
        }
        )
    }


    override def processResponse(response: Future[HttpResponse]): Future[OAuth2BearerToken] = {
      // todo
     /* val httpResponse = response.value.get.get

      val future: Future[Any] = response.map((r: HttpResponse) => handleStatuses(r))

      response.map(handleStatuses)*/

      val eventualToken = response
        .flatMap(_.entity.toStrict(2 seconds))
        .map(r => {
          // This is unsafe -> hint; nObject.get
          // Errors should be handled

          val responseAsString = (r.getData().utf8String)
          println(responseAsString)
          val nObject = new JSONObject(responseAsString)
          OAuth2BearerToken(nObject.get("access_token").toString)
        })
      eventualToken

    }


  }

  val clientId = "d76a6461e4fa42eda58420eafb25f76c"
  val clientSecret = "32a3c3a6f5ed475586aefaecd0ae39f0"
  val tokenEndpoint = "https://accounts.spotify.com/api/token"

  val tokenAcquirer = new SpotifyTokenAcquisitor(clientId, clientSecret, tokenEndpoint)

  private val eventualTokenWrapper: Future[Any] = tokenAcquirer.acquireToken()

  import scala.concurrent.ExecutionContext.Implicits.global

  eventualTokenWrapper map {
    case t: OAuth2BearerToken => println(t.token)
    case _ => println(" Unknown")
  }

}
