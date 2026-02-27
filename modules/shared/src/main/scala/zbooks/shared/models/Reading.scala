package zbooks.shared.models

import zio.json.*

// Dates are ISO-8601 strings ("YYYY-MM-DD") to avoid scala-java-time on JS
case class Reading(
    id: Long,
    bookId: Long,
    startDate: String,
    endDate: String,
) derives JsonCodec

case class NewReading(
    bookId: Long,
    startDate: String,
    endDate: String,
) derives JsonCodec
