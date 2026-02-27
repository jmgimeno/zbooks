package zbooks.backend.http

import zbooks.backend.service.BookService
import zbooks.shared.api.*
import zio.*
import zio.http.*
import zio.json.*

object BookRoutes:

  def routes(bookService: BookService): Routes[Any, Nothing] =
    Routes(
      // GET /api/books
      Method.GET / "api" / "books" -> handler {
        bookService.getAllBooks
          .map(books => Response.json(books.toJson))
          .catchAll(err => ZIO.succeed(serverError(err.getMessage)))
      },

      // POST /api/books
      Method.POST / "api" / "books" -> handler { (req: Request) =>
        (for
          body   <- req.body.asString
          result <- ZIO.fromEither(body.fromJson[CreateBookRequest])
                      .mapError(msg => new Exception(s"Invalid request body: $msg"))
          book   <- bookService.createBook(result)
        yield Response.json(book.toJson).status(Status.Created))
        .catchAll { err =>
          ZIO.succeed(err match
            case _: IllegalArgumentException => badRequest(err.getMessage)
            case _                           => serverError(err.getMessage)
          )
        }
      },

      // GET /api/books/:id
      Method.GET / "api" / "books" / long("id") -> handler { (id: Long, _: Request) =>
        bookService.getBook(id)
          .map {
            case Some(bwr) => Response.json(bwr.toJson)
            case None      => notFound(id)
          }
          .catchAll(err => ZIO.succeed(serverError(err.getMessage)))
      },

      // PUT /api/books/:id
      Method.PUT / "api" / "books" / long("id") -> handler { (id: Long, req: Request) =>
        (for
          body   <- req.body.asString
          result <- ZIO.fromEither(body.fromJson[UpdateBookRequest])
                      .mapError(msg => new Exception(s"Invalid request body: $msg"))
          book   <- bookService.updateBook(id, result)
        yield book match
          case Some(b) => Response.json(b.toJson)
          case None    => notFound(id)
        ).catchAll { err =>
          ZIO.succeed(err match
            case _: IllegalArgumentException => badRequest(err.getMessage)
            case _                           => serverError(err.getMessage)
          )
        }
      },

      // DELETE /api/books/:id
      Method.DELETE / "api" / "books" / long("id") -> handler { (id: Long, _: Request) =>
        bookService.deleteBook(id)
          .map {
            case true  => Response.status(Status.NoContent)
            case false => notFound(id)
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

  private def notFound(id: Long): Response =
    errorJson(s"Book $id not found").status(Status.NotFound)
