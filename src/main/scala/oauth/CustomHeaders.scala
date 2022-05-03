package oauth

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}

import scala.util.Try

class CustomContentTypeHeader(value: String) extends ModeledCustomHeader[CustomContentTypeHeader] {

  override def companion: ModeledCustomHeaderCompanion[CustomContentTypeHeader] = CustomContentTypeHeader

  override def value(): String = value

  override def renderInRequests(): Boolean = true

  override def renderInResponses(): Boolean = true
}

object CustomContentTypeHeader extends ModeledCustomHeaderCompanion[CustomContentTypeHeader] {
  val defaultValue = "multipart/form-data; boundary=---011000010111000001101001"

  override def name: String = "content-type"

  override def parse(value: String): Try[CustomContentTypeHeader] = Try(new CustomContentTypeHeader(defaultValue))
}