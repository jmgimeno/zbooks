package zbooks.backend.db

import com.augustnagro.magnum.*
import zio.*

import javax.sql.DataSource

@Table(H2DbType, SqlNameMapper.CamelToSnakeCase)
case class BookRow(
    @Id id: Long,
    name: String,
    author: String,
    editor: String,
    year: Int,
    evaluation: Option[Int],
) derives DbCodec

case class NewBookRow(
    name: String,
    author: String,
    editor: String,
    year: Int,
    evaluation: Option[Int],
) derives DbCodec

object BookRepo:

  private val repo = Repo[NewBookRow, BookRow, Long]

  def findAll(ds: DataSource): Task[List[BookRow]] =
    ZIO.attempt(connect(ds)(repo.findAll.toList))

  def findById(ds: DataSource, id: Long): Task[Option[BookRow]] =
    ZIO.attempt(connect(ds)(repo.findById(id)))

  def insert(ds: DataSource, row: NewBookRow): Task[BookRow] =
    ZIO.attempt(connect(ds)(repo.insertReturning(row)))

  def update(ds: DataSource, row: BookRow): Task[Unit] =
    ZIO.attempt(connect(ds)(repo.update(row)))

  def delete(ds: DataSource, id: Long): Task[Unit] =
    ZIO.attempt(connect(ds)(repo.deleteById(id)))

  val layer: ZLayer[DataSource, Nothing, BookRepo] =
    ZLayer.fromFunction(BookRepo(_))

case class BookRepo(ds: DataSource):
  def findAll: Task[List[BookRow]]            = BookRepo.findAll(ds)
  def findById(id: Long): Task[Option[BookRow]] = BookRepo.findById(ds, id)
  def insert(row: NewBookRow): Task[BookRow]  = BookRepo.insert(ds, row)
  def update(row: BookRow): Task[Unit]        = BookRepo.update(ds, row)
  def delete(id: Long): Task[Unit]            = BookRepo.delete(ds, id)
