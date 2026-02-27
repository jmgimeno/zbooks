package zbooks.backend

import zio.*

case class AppConfig(
    port: Int,
    dbPath: String,
    staticDir: String,
)

object AppConfig:
  val layer: ZLayer[Any, Nothing, AppConfig] =
    ZLayer.succeed(
      AppConfig(
        port      = sys.env.getOrElse("ZBOOKS_PORT", "8080").toInt,
        dbPath    = sys.env.getOrElse("ZBOOKS_DB_PATH", "/data/zbooks"),
        staticDir = sys.env.getOrElse("ZBOOKS_STATIC_DIR", "/app/static"),
      )
    )
