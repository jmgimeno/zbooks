package zbooks.backend.service

import zbooks.backend.db.{BookRepo, NewReadingRow, ReadingRepo, ReadingRow}
import zbooks.shared.api.*
import zbooks.shared.models.*
import zio.*

import java.sql.{Date => SqlDate}
import java.time.LocalDate

class ReadingService(bookRepo: BookRepo, readingRepo: ReadingRepo):

  def addReading(bookId: Long, req: CreateReadingRequest): Task[Option[Reading]] =
    for
      maybeBook <- bookRepo.findById(bookId)
      result <- ZIO.foreach(maybeBook) { _ =>
        for
          start <- parseDate(req.startDate)
          end   <- parseDate(req.endDate)
          _     <- validateDates(start, end)
          row   <- readingRepo.insert(NewReadingRow(bookId, SqlDate.valueOf(start), SqlDate.valueOf(end)))
        yield toReading(row)
      }
    yield result

  def updateReading(bookId: Long, readingId: Long, req: UpdateReadingRequest): Task[Option[Reading]] =
    for
      maybeReading <- readingRepo.findById(readingId)
      result <- ZIO.foreach(maybeReading.filter(_.bookId == bookId)) { existing =>
        for
          start   <- parseDate(req.startDate)
          end     <- parseDate(req.endDate)
          _       <- validateDates(start, end)
          updated  = existing.copy(startDate = SqlDate.valueOf(start), endDate = SqlDate.valueOf(end))
          _       <- readingRepo.update(updated)
        yield toReading(updated)
      }
    yield result

  def deleteReading(bookId: Long, readingId: Long): Task[Boolean] =
    readingRepo.findById(readingId).flatMap {
      case None                          => ZIO.succeed(false)
      case Some(r) if r.bookId != bookId => ZIO.succeed(false)
      case Some(_)                       => readingRepo.delete(readingId).as(true)
    }

  private def parseDate(s: String): Task[LocalDate] =
    ZIO.attempt(LocalDate.parse(s))
      .mapError(_ => IllegalArgumentException(s"Invalid date format: '$s'. Expected YYYY-MM-DD"))

  private def validateDates(start: LocalDate, end: LocalDate): Task[Unit] =
    if end.isBefore(start) then
      ZIO.fail(IllegalArgumentException(s"end_date ($end) must be >= start_date ($start)"))
    else ZIO.unit

  private def toReading(row: ReadingRow): Reading =
    Reading(row.id, row.bookId, row.startDate.toLocalDate.toString, row.endDate.toLocalDate.toString)

object ReadingService:
  val layer: ZLayer[BookRepo & ReadingRepo, Nothing, ReadingService] =
    ZLayer {
      for
        br <- ZIO.service[BookRepo]
        rr <- ZIO.service[ReadingRepo]
      yield ReadingService(br, rr)
    }
