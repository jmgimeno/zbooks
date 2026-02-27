package zbooks.backend.db

import com.augustnagro.magnum.*
import zio.*

import java.sql.{Date => SqlDate}
import java.time.LocalDate
import javax.sql.DataSource

@Table(H2DbType, SqlNameMapper.CamelToSnakeCase)
case class ReadingRow(
    @Id id: Long,
    bookId: Long,
    startDate: SqlDate,
    endDate: SqlDate,
) derives DbCodec

case class NewReadingRow(
    bookId: Long,
    startDate: SqlDate,
    endDate: SqlDate,
) derives DbCodec

// Convenience conversions used by ReadingService
extension (r: ReadingRow)
  def startLocalDate: LocalDate = r.startDate.toLocalDate
  def endLocalDate: LocalDate   = r.endDate.toLocalDate

object ReadingRepo:

  private val repo = Repo[NewReadingRow, ReadingRow, Long]

  def findByBookId(ds: DataSource, bookId: Long): Task[List[ReadingRow]] =
    ZIO.attempt(
      connect(ds) {
        sql"SELECT * FROM reading_row WHERE book_id = $bookId".query[ReadingRow].run().toList
      }
    )

  def findById(ds: DataSource, id: Long): Task[Option[ReadingRow]] =
    ZIO.attempt(connect(ds)(repo.findById(id)))

  def insert(ds: DataSource, row: NewReadingRow): Task[ReadingRow] =
    ZIO.attempt(connect(ds)(repo.insertReturning(row)))

  def update(ds: DataSource, row: ReadingRow): Task[Unit] =
    ZIO.attempt(connect(ds)(repo.update(row)))

  def delete(ds: DataSource, id: Long): Task[Unit] =
    ZIO.attempt(connect(ds)(repo.deleteById(id)))

  val layer: ZLayer[DataSource, Nothing, ReadingRepo] =
    ZLayer.fromFunction(ReadingRepo(_))

case class ReadingRepo(ds: DataSource):
  def findByBookId(bookId: Long): Task[List[ReadingRow]] = ReadingRepo.findByBookId(ds, bookId)
  def findById(id: Long): Task[Option[ReadingRow]]       = ReadingRepo.findById(ds, id)
  def insert(row: NewReadingRow): Task[ReadingRow]       = ReadingRepo.insert(ds, row)
  def update(row: ReadingRow): Task[Unit]                = ReadingRepo.update(ds, row)
  def delete(id: Long): Task[Unit]                       = ReadingRepo.delete(ds, id)
