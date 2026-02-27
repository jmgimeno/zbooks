package zbooks.shared.api

import zbooks.shared.models.*
import zio.json.*

case class BookWithReadings(
    book: Book,
    readings: List[Reading],
) derives JsonCodec

case class CreateBookRequest(
    name: String,
    author: String,
    editor: String,
    year: Int,
    evaluation: Option[Int],
) derives JsonCodec

case class UpdateBookRequest(
    name: String,
    author: String,
    editor: String,
    year: Int,
    evaluation: Option[Int],
) derives JsonCodec

case class CreateReadingRequest(
    startDate: String,
    endDate: String,
) derives JsonCodec

case class UpdateReadingRequest(
    startDate: String,
    endDate: String,
) derives JsonCodec

case class ErrorResponse(error: String) derives JsonCodec
