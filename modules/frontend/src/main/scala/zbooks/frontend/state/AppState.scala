package zbooks.frontend.state

import com.raquo.laminar.api.L.*
import zbooks.shared.api.BookWithReadings
import zbooks.shared.models.Book

object AppState:
  val booksVar: Var[List[BookWithReadings]] = Var(List.empty)
  val loadingVar: Var[Boolean]              = Var(false)
  val errorVar: Var[Option[String]]         = Var(None)
