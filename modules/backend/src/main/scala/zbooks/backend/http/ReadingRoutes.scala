package zbooks.backend.http

import zbooks.backend.service.ReadingService
import zbooks.shared.api.*
import zio.*
import zio.http.*
import zio.json.*

object ReadingRoutes:

  def routes(readingService: ReadingService): Routes[Any, Nothing] =
    Routes(
      // POST /api/books/:bookId/readings
      Method.POST / "api" / "books" / long("bookId") / "readings" ->
        handler { (bookId: Long, req: Request) =>
          (for
            body    <- req.body.asString
            result  <- ZIO.fromEither(body.fromJson[CreateReadingRequest])
                         .mapError(msg => new Exception(s"Invalid request body: $msg"))
            reading <- readingService.addReading(bookId, result)
          yield reading match
            case Some(r) => Response.json(r.toJson).status(Status.Created)
            case None    => notFoundBook(bookId)
          ).catchAll { err =>
            ZIO.succeed(err match
              case _: IllegalArgumentException => badRequest(err.getMessage)
              case _                           => serverError(err.getMessage)
            )
          }
        },

      // PUT /api/books/:bookId/readings/:id
      Method.PUT / "api" / "books" / long("bookId") / "readings" / long("id") ->
        handler { (bookId: Long, id: Long, req: Request) =>
          (for
            body    <- req.body.asString
            result  <- ZIO.fromEither(body.fromJson[UpdateReadingRequest])
                         .mapError(msg => new Exception(s"Invalid request body: $msg"))
            reading <- readingService.updateReading(bookId, id, result)
          yield reading match
            case Some(r) => Response.json(r.toJson)
            case None    => notFoundReading(id)
          ).catchAll { err =>
            ZIO.succeed(err match
              case _: IllegalArgumentException => badRequest(err.getMessage)
              case _                           => serverError(err.getMessage)
            )
          }
        },

      // DELETE /api/books/:bookId/readings/:id
      Method.DELETE / "api" / "books" / long("bookId") / "readings" / long("id") ->
        handler { (bookId: Long, id: Long, _: Request) =>
          readingService.deleteReading(bookId, id)
            .map {
              case true  => Response.status(Status.NoContent)
              case false => notFoundReading(id)
            }
            .catchAll(err => ZIO.succeed(serverError(err.getMessage)))
        },
    )

  private def errorJson(message: String): Response =
    Response.json(ErrorResponse(message).toJson)

  private def badRequest(message: String): Response =
    errorJson(message).status(Status.BadRequest)

  private def serverError(message: String): Response =
    errorJson(message).status(Status.InternalServerError)

  private def notFoundBook(bookId: Long): Response =
    errorJson(s"Book $bookId not found").status(Status.NotFound)

  private def notFoundReading(id: Long): Response =
    errorJson(s"Reading $id not found").status(Status.NotFound)
