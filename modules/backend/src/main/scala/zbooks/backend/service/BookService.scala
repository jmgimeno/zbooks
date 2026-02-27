package zbooks.backend.service

import zbooks.backend.db.{BookRepo, NewBookRow, BookRow, ReadingRepo}
import zbooks.shared.api.*
import zbooks.shared.models.*
import zio.*

class BookService(bookRepo: BookRepo, readingRepo: ReadingRepo):

  def getAllBooks: Task[List[BookWithReadings]] =
    for
      books    <- bookRepo.findAll
      withReadings <- ZIO.foreach(books)(book =>
        readingRepo.findByBookId(book.id).map(rs => BookWithReadings(toBook(book), rs.map(toReading)))
      )
    yield withReadings

  def getBook(id: Long): Task[Option[BookWithReadings]] =
    for
      maybeBook <- bookRepo.findById(id)
      result <- ZIO.foreach(maybeBook) { book =>
        readingRepo.findByBookId(book.id).map(rs => BookWithReadings(toBook(book), rs.map(toReading)))
      }
    yield result

  def createBook(req: CreateBookRequest): Task[Book] =
    for
      _ <- validateEvaluation(req.evaluation)
      _ <- validateYear(req.year)
      row <- bookRepo.insert(NewBookRow(req.name, req.author, req.editor, req.year, req.evaluation))
    yield toBook(row)

  def updateBook(id: Long, req: UpdateBookRequest): Task[Option[Book]] =
    for
      _ <- validateEvaluation(req.evaluation)
      _ <- validateYear(req.year)
      maybeBook <- bookRepo.findById(id)
      result <- ZIO.foreach(maybeBook) { existing =>
        val updated = existing.copy(
          name       = req.name,
          author     = req.author,
          editor     = req.editor,
          year       = req.year,
          evaluation = req.evaluation,
        )
        bookRepo.update(updated).as(toBook(updated))
      }
    yield result

  def deleteBook(id: Long): Task[Boolean] =
    bookRepo.findById(id).flatMap {
      case None    => ZIO.succeed(false)
      case Some(_) => bookRepo.delete(id).as(true)
    }

  private def validateEvaluation(eval: Option[Int]): Task[Unit] =
    eval match
      case Some(v) if v < 1 || v > 5 =>
        ZIO.fail(IllegalArgumentException(s"Evaluation must be between 1 and 5, got $v"))
      case _ => ZIO.unit

  private def validateYear(year: Int): Task[Unit] =
    if year < 1000 || year > 9999 then
      ZIO.fail(IllegalArgumentException(s"Year $year is not plausible"))
    else ZIO.unit

  private def toBook(row: BookRow): Book =
    Book(row.id, row.name, row.author, row.editor, row.year, row.evaluation)

  private def toReading(row: zbooks.backend.db.ReadingRow): Reading =
    Reading(row.id, row.bookId, row.startDate.toLocalDate.toString, row.endDate.toLocalDate.toString)

object BookService:
  val layer: ZLayer[BookRepo & ReadingRepo, Nothing, BookService] =
    ZLayer {
      for
        br <- ZIO.service[BookRepo]
        rr <- ZIO.service[ReadingRepo]
      yield BookService(br, rr)
    }
