package zbooks.backend.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import zbooks.backend.AppConfig
import zio.*

import javax.sql.DataSource

object DatabaseSetup:

  def make(dbPath: String): ZIO[Scope, Throwable, HikariDataSource] =
    ZIO.acquireRelease(
      ZIO.attempt {
        val config = HikariConfig()
        // NON_KEYWORDS=YEAR: H2 2.x reserved YEAR as a keyword; un-reserve it for our column name
        config.setJdbcUrl(s"jdbc:h2:file:$dbPath;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
        config.setDriverClassName("org.h2.Driver")
        config.setMaximumPoolSize(10)
        config.setMinimumIdle(2)
        config.setConnectionTimeout(30000)
        val ds = HikariDataSource(config)
        runSchema(ds)
        ds
      }
    )(ds => ZIO.attempt(ds.close()).orDie)

  private def runSchema(ds: DataSource): Unit =
    val stream = getClass.getResourceAsStream("/schema.sql")
    if stream == null then throw RuntimeException("schema.sql not found on classpath")
    val sql = scala.io.Source.fromInputStream(stream).mkString
    val conn = ds.getConnection()
    try
      val stmt = conn.createStatement()
      sql.split(";").map(_.trim).filter(_.nonEmpty).foreach { s =>
        stmt.execute(s)
      }
      stmt.close()
    finally
      conn.close()

  val layer: ZLayer[AppConfig, Throwable, DataSource] =
    ZLayer.scoped {
      for
        cfg <- ZIO.service[AppConfig]
        ds  <- make(cfg.dbPath)
      yield ds: DataSource
    }
