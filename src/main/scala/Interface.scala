import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.http.scaladsl.model.{HttpHeader, HttpMethod, HttpMethods, HttpRequest,
  HttpResponse, MediaRanges, RequestEntity, ResponseEntity, StatusCodes, headers}
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}

  abstract class TokenAcquisitor[TokenWrapper](val clientId: String,
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
      case HttpResponse(StatusCodes.OK, _, entity, _)=> handleOK(entity)
      case HttpResponse(_, headers, entity, _) => handleBad(httpResponse)
    }

    def processResponse(response: Future[HttpResponse]): Future[Any] = {
      response.map(handleStatuses)
    }
}
