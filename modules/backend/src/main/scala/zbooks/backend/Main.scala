package zbooks.backend

import zbooks.backend.db.{BookRepo, DatabaseSetup, ReadingRepo}
import zbooks.backend.http.{BookRoutes, ReadingRoutes, StaticRoutes}
import zbooks.backend.service.{BookService, ReadingService}
import zio.*
import zio.http.*

object Main extends ZIOAppDefault:

  override def run: ZIO[Any, Throwable, Unit] =
    app.provide(
      AppConfig.layer,
      DatabaseSetup.layer,
      BookRepo.layer,
      ReadingRepo.layer,
      BookService.layer,
      ReadingService.layer,
      serverConfigLayer,
      Server.live,
    )

  private val app: ZIO[AppConfig & BookService & ReadingService & Server, Throwable, Unit] =
    for
      cfg            <- ZIO.service[AppConfig]
      bookService    <- ZIO.service[BookService]
      readingService <- ZIO.service[ReadingService]
      routes          = BookRoutes.routes(bookService) ++
                        ReadingRoutes.routes(readingService) ++
                        StaticRoutes.routes(cfg.staticDir)
      _              <- ZIO.logInfo(s"Starting ZBooks on port ${cfg.port}")
      _              <- Server.serve(routes)
    yield ()

  private val serverConfigLayer: ZLayer[AppConfig, Nothing, Server.Config] =
    ZLayer.fromZIO(
      ZIO.service[AppConfig].map(cfg => Server.Config.default.port(cfg.port))
    )
