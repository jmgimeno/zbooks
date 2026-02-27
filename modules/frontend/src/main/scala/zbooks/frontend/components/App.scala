package zbooks.frontend.components

import com.raquo.laminar.api.L.*
import zbooks.frontend.Router.*
import zbooks.frontend.{BookDetail, BookList, Router}
import zbooks.frontend.api.ApiClient
import zbooks.frontend.state.AppState

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object App:

  def apply(): HtmlElement =
    div(
      cls := "min-h-screen bg-gray-50",
      header(),
      mainTag(
        cls := "pb-12",
        child <-- Router.currentPage.signal.map {
          case BookList       => BookListPage()
          case BookDetail(id) => BookDetailPage(id)
        },
      ),
    )

  private def header(): HtmlElement =
    navTag(
      cls := "bg-white border-b border-gray-200 shadow-sm",
      div(
        cls := "max-w-6xl mx-auto px-4 py-4 flex items-center justify-between",
        a(
          cls  := "flex items-center gap-2 text-xl font-bold text-gray-900 hover:text-blue-600 transition-colors cursor-pointer",
          onClick --> { _ => Router.navigate(BookList) },
          span("📚"),
          "ZBooks",
        ),
        span(cls := "text-xs text-gray-400", "Personal Book Tracker"),
      ),
    )

  /** Load all books on application startup */
  def loadInitialData(): Unit =
    AppState.loadingVar.set(true)
    ApiClient.getBooks().onComplete {
      case Success(Right(books)) =>
        AppState.booksVar.set(books)
        AppState.loadingVar.set(false)
      case Success(Left(err)) =>
        AppState.errorVar.set(Some(err))
        AppState.loadingVar.set(false)
      case Failure(ex) =>
        AppState.errorVar.set(Some(ex.getMessage))
        AppState.loadingVar.set(false)
    }
