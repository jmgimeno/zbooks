package zbooks.backend.http

import zio.*
import zio.http.*

import java.io.File
import java.nio.file.{Files as JFiles, Paths as JPaths}

object StaticRoutes:

  def routes(staticDir: String): Routes[Any, Nothing] =
    Routes(
      // GET /assets/* — serve static files
      Method.GET / "assets" / trailing -> handler { (path: zio.http.Path, _: Request) =>
        val segments = path.segments.mkString("/")
        val filePath = JPaths.get(staticDir, "assets", segments)
        serveFile(filePath.toFile)
      },

      // Catch-all GET: serve index.html (SPA hash routing)
      Method.GET / trailing -> handler { (_: zio.http.Path, _: Request) =>
        serveFile(new File(staticDir, "index.html"))
      },
    )

  private def serveFile(file: File): UIO[Response] =
    ZIO.attempt {
      if !file.exists() || !file.isFile then
        Response.status(Status.NotFound)
      else
        val bytes = JFiles.readAllBytes(file.toPath)
        val ext   = file.getName.split('.').lastOption.getOrElse("")
        val mediaType = MediaType.forFileExtension(ext)
          .getOrElse(MediaType.application.`octet-stream`)
        Response(
          status  = Status.Ok,
          headers = Headers(Header.ContentType(mediaType)),
          body    = Body.fromArray(bytes),
        )
    }.catchAll(_ => ZIO.succeed(Response.status(Status.InternalServerError)))
