package zbooks.shared.models

import zio.json.*

case class Book(
    id: Long,
    name: String,
    author: String,
    editor: String,
    year: Int,
    evaluation: Option[Int],
) derives JsonCodec

case class NewBook(
    name: String,
    author: String,
    editor: String,
    year: Int,
    evaluation: Option[Int],
) derives JsonCodec
