package zbooks.frontend.api

import org.scalajs.dom
import org.scalajs.dom.{HttpMethod, RequestInit}
import zbooks.shared.api.*
import zbooks.shared.models.*
import zio.json.*

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits.*

object ApiClient:

  private given ExecutionContext = ExecutionContext.global

  // ─── Books ────────────────────────────────────────────────────────────────

  def getBooks(): Future[Either[String, List[BookWithReadings]]] =
    fetchJson[List[BookWithReadings]]("GET", "/api/books", None)

  def getBook(id: Long): Future[Either[String, BookWithReadings]] =
    fetchJson[BookWithReadings]("GET", s"/api/books/$id", None)

  def createBook(req: CreateBookRequest): Future[Either[String, Book]] =
    fetchJson[Book]("POST", "/api/books", Some(req.toJson))

  def updateBook(id: Long, req: UpdateBookRequest): Future[Either[String, Book]] =
    fetchJson[Book]("PUT", s"/api/books/$id", Some(req.toJson))

  def deleteBook(id: Long): Future[Either[String, Unit]] =
    fetchNoBody("DELETE", s"/api/books/$id")

  // ─── Readings ─────────────────────────────────────────────────────────────

  def createReading(bookId: Long, req: CreateReadingRequest): Future[Either[String, Reading]] =
    fetchJson[Reading]("POST", s"/api/books/$bookId/readings", Some(req.toJson))

  def updateReading(bookId: Long, id: Long, req: UpdateReadingRequest): Future[Either[String, Reading]] =
    fetchJson[Reading]("PUT", s"/api/books/$bookId/readings/$id", Some(req.toJson))

  def deleteReading(bookId: Long, id: Long): Future[Either[String, Unit]] =
    fetchNoBody("DELETE", s"/api/books/$bookId/readings/$id")

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private def fetchJson[A: JsonDecoder](
      method: String,
      url: String,
      body: Option[String],
  ): Future[Either[String, A]] =
    val init = buildInit(method, body)
    dom.fetch(url, init).toFuture
      .flatMap { resp =>
        resp.text().toFuture.map { text =>
          if resp.ok then
            text.fromJson[A].left.map(e => s"Decode error: $e (body: $text)")
          else
            val msg = text.fromJson[ErrorResponse].fold(_ => text, _.error)
            Left(s"HTTP ${resp.status}: $msg")
        }
      }
      .recover { case ex => Left(s"Network error: ${ex.getMessage}") }

  private def fetchNoBody(method: String, url: String): Future[Either[String, Unit]] =
    val init = buildInit(method, None)
    dom.fetch(url, init).toFuture
      .map { resp =>
        if resp.ok then Right(())
        else Left(s"HTTP ${resp.status}")
      }
      .recover { case ex => Left(s"Network error: ${ex.getMessage}") }

  private def buildInit(method: String, body: Option[String]): RequestInit =
    val init = new dom.RequestInit {}
    init.method = method.asInstanceOf[HttpMethod]
    init.headers = js.Dictionary("Content-Type" -> "application/json")
    body.foreach(b => init.body = b)
    init
