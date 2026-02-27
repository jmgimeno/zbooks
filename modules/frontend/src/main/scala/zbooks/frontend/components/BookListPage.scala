package zbooks.frontend.components

import com.raquo.laminar.api.L.*
import zbooks.frontend.Router
import zbooks.frontend.api.ApiClient
import zbooks.frontend.state.AppState
import zbooks.shared.api.BookWithReadings

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object BookListPage:

  def apply(): HtmlElement =
    val searchVar  = Var("")
    val showAddVar = Var(false)

    val filteredBooks: Signal[List[BookWithReadings]] =
      searchVar.signal.combineWith(AppState.booksVar.signal).map { (query, books) =>
        val q = query.trim.toLowerCase
        if q.isEmpty then books
        else books.filter { bwr =>
          bwr.book.name.toLowerCase.contains(q) ||
          bwr.book.author.toLowerCase.contains(q) ||
          bwr.book.editor.toLowerCase.contains(q)
        }
      }

    div(
      cls := "max-w-6xl mx-auto px-4 py-8",

      // ── Header row ──────────────────────────────────────────────────────
      div(
        cls := "flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between mb-8",
        div(
          cls := "relative flex-1 max-w-md",
          input(
            tpe         := "text",
            cls         := "w-full pl-10 pr-4 py-2 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500",
            placeholder := "Search books…",
            value       <-- searchVar.signal,
            onInput.mapToValue --> searchVar.writer,
          ),
          span(cls := "absolute left-3 top-2.5 text-gray-400 text-sm pointer-events-none", "🔍"),
        ),
        button(
          tpe := "button",
          cls := "flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-600 text-white hover:bg-blue-700 transition-colors shadow-sm",
          onClick --> { _ => showAddVar.set(true) },
          span("＋"),
          "Add Book",
        ),
      ),

      // ── Loading state ────────────────────────────────────────────────────
      child.maybe <-- AppState.loadingVar.signal.map { loading =>
        Option.when(loading)(div(cls := "text-center py-16 text-gray-500", "Loading books…"))
      },

      // ── Error banner ─────────────────────────────────────────────────────
      child.maybe <-- AppState.errorVar.signal.map(_.map { err =>
        div(
          cls := "bg-red-50 border border-red-200 rounded-xl p-4 mb-6 text-red-700",
          s"Error: $err",
        )
      }),

      // ── Empty state ──────────────────────────────────────────────────────
      child.maybe <-- filteredBooks.combineWith(AppState.loadingVar.signal).map { (books, loading) =>
        Option.when(books.isEmpty && !loading)(
          div(
            cls := "text-center py-24",
            div(cls := "text-5xl mb-4", "📚"),
            p(cls := "text-xl text-gray-500 mb-2", "No books yet"),
            p(cls := "text-gray-400", "Click \u201cAdd Book\u201d to get started"),
          )
        )
      },

      // ── Book grid ────────────────────────────────────────────────────────
      div(
        cls := "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6",
        children <-- filteredBooks.map(_.map(bookCard)),
      ),

      // ── Add modal ────────────────────────────────────────────────────────
      child.maybe <-- showAddVar.signal.map { show =>
        Option.when(show)(BookForm.addModal(() => showAddVar.set(false)))
      },
    )

  private def bookCard(bwr: BookWithReadings): HtmlElement =
    val b = bwr.book
    div(
      cls := "bg-white rounded-2xl shadow-sm border border-gray-100 p-5 hover:shadow-md hover:-translate-y-0.5 transition-all cursor-pointer",
      onClick --> { _ => Router.navigate(zbooks.frontend.BookDetail(b.id)) },
      div(
        cls := "flex flex-col h-full",
        div(
          cls := "flex-1",
          h3(cls := "font-semibold text-gray-900 text-lg leading-snug mb-1 line-clamp-2", b.name),
          p(cls := "text-gray-600 text-sm mb-0.5", b.author),
          p(cls := "text-gray-400 text-xs mb-3", s"${b.editor} · ${b.year}"),
        ),
        div(
          cls := "flex items-center justify-between mt-auto pt-3 border-t border-gray-50",
          StarRating.display(b.evaluation),
          span(
            cls := "text-xs text-gray-400",
            s"${bwr.readings.length} reading${if bwr.readings.length == 1 then "" else "s"}",
          ),
        ),
      ),
    )
