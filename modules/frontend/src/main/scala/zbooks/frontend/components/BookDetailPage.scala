package zbooks.frontend.components

import com.raquo.laminar.api.L.*
import zbooks.frontend.Router
import zbooks.frontend.api.ApiClient
import zbooks.frontend.state.AppState
import zbooks.shared.api.BookWithReadings
import zbooks.shared.models.Reading

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object BookDetailPage:

  def apply(bookId: Long): HtmlElement =
    val showEditVar    = Var(false)
    val showAddReadVar = Var(false)
    val editReadVar    = Var(Option.empty[Reading])
    val deletingVar    = Var(false)
    val errVar         = Var(Option.empty[String])

    val bookSignal: Signal[Option[BookWithReadings]] =
      AppState.booksVar.signal.map(_.find(_.book.id == bookId))

    div(
      cls := "max-w-3xl mx-auto px-4 py-8",

      // ── Back button ──────────────────────────────────────────────────────
      button(
        tpe := "button",
        cls := "flex items-center gap-2 text-gray-500 hover:text-gray-700 mb-6 transition-colors",
        onClick --> { _ => Router.navigate(zbooks.frontend.BookList) },
        "← Back to books",
      ),

      // ── Book not found ───────────────────────────────────────────────────
      child.maybe <-- bookSignal.map {
        case None => Some(div(cls := "text-center py-16 text-gray-500", "Book not found."))
        case _    => None
      },

      // ── Main content ─────────────────────────────────────────────────────
      child.maybe <-- bookSignal.map(_.map { bwr =>
        val b = bwr.book
        div(
          cls := "space-y-8",

          // Book header card
          div(
            cls := "bg-white rounded-2xl shadow-sm border border-gray-100 p-6",
            div(
              cls := "flex items-start justify-between gap-4",
              div(
                cls := "flex-1",
                h1(cls := "text-2xl font-bold text-gray-900 mb-1", b.name),
                p(cls := "text-gray-600 mb-0.5", b.author),
                p(cls := "text-gray-400 text-sm", s"${b.editor} · ${b.year}"),
                div(cls := "mt-3", StarRating.display(b.evaluation)),
              ),
              div(
                cls := "flex gap-2 flex-shrink-0",
                button(
                  tpe := "button",
                  cls := "px-3 py-1.5 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 transition-colors",
                  "Edit",
                  onClick --> { _ => showEditVar.set(true) },
                ),
                button(
                  tpe      := "button",
                  cls      := "px-3 py-1.5 text-sm rounded-lg border border-red-200 text-red-600 hover:bg-red-50 transition-colors disabled:opacity-50",
                  disabled <-- deletingVar.signal,
                  child.text <-- deletingVar.signal.map(d => if d then "Deleting..." else "Delete"),
                  onClick --> { _ =>
                    if org.scalajs.dom.window.confirm(s"Delete '${b.name}'?") then
                      deletingVar.set(true)
                      ApiClient.deleteBook(b.id).onComplete {
                        case Success(Right(_)) =>
                          AppState.booksVar.update(_.filterNot(_.book.id == b.id))
                          Router.navigate(zbooks.frontend.BookList)
                        case Success(Left(err)) =>
                          errVar.set(Some(err))
                          deletingVar.set(false)
                        case Failure(ex) =>
                          errVar.set(Some(ex.getMessage))
                          deletingVar.set(false)
                      }
                  },
                ),
              ),
            ),
          ),

          // Error banner
          child.maybe <-- errVar.signal.map(_.map(err =>
            div(cls := "bg-red-50 border border-red-200 rounded-xl p-3 text-sm text-red-700", err)
          )),

          // Readings section
          div(
            cls := "bg-white rounded-2xl shadow-sm border border-gray-100 p-6",
            div(
              cls := "flex items-center justify-between mb-4",
              h2(cls := "text-lg font-semibold text-gray-900", "Reading Sessions"),
              button(
                tpe := "button",
                cls := "flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-lg bg-green-600 text-white hover:bg-green-700 transition-colors",
                onClick --> { _ => showAddReadVar.set(true) },
                "+ Add",
              ),
            ),

            // Add reading inline form
            child.maybe <-- showAddReadVar.signal.map(show =>
              Option.when(show)(ReadingForm.addForm(bookId, () => showAddReadVar.set(false)))
            ),

            // Readings list
            if bwr.readings.isEmpty then
              div(cls := "text-center py-8 text-gray-400", "No reading sessions yet.")
            else
              div(
                cls := "space-y-2 mt-2",
                bwr.readings.sortBy(_.startDate).map(r => readingRow(bookId, r, editReadVar)),
              ),
          ),
        )
      }),

      // ── Edit book modal ──────────────────────────────────────────────────
      child.maybe <-- showEditVar.signal.combineWith(bookSignal).map {
        case (true, Some(bwr)) =>
          Some(BookForm.editModal(bwr, () => showEditVar.set(false)))
        case _ => None
      },

      // ── Edit reading form (appears below the readings section) ────────────
      child.maybe <-- editReadVar.signal.map(_.map(r =>
        ReadingForm.editForm(bookId, r, () => editReadVar.set(None))
      )),
    )

  private def readingRow(
      bookId: Long,
      reading: Reading,
      editReadVar: Var[Option[Reading]],
  ): HtmlElement =
    val deletingVar = Var(false)

    div(
      cls := "flex items-center justify-between py-2.5 px-3 rounded-lg hover:bg-gray-50 group",
      div(
        cls := "flex items-center gap-3",
        span(cls := "text-gray-400 text-sm", "📖"),
        span(
          cls := "text-gray-700 text-sm",
          s"${reading.startDate}  \u2192  ${reading.endDate}",
        ),
      ),
      div(
        cls := "flex gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity",
        button(
          tpe := "button",
          cls := "px-2 py-1 text-xs rounded border border-gray-200 text-gray-500 hover:bg-gray-100 transition-colors",
          "Edit",
          onClick --> { _ => editReadVar.set(Some(reading)) },
        ),
        button(
          tpe      := "button",
          cls      := "px-2 py-1 text-xs rounded border border-red-100 text-red-500 hover:bg-red-50 transition-colors disabled:opacity-50",
          disabled <-- deletingVar.signal,
          "Delete",
          onClick --> { _ =>
            deletingVar.set(true)
            ApiClient.deleteReading(bookId, reading.id).onComplete {
              case Success(Right(_)) =>
                AppState.booksVar.update(books =>
                  books.map { bwr =>
                    if bwr.book.id == bookId then
                      bwr.copy(readings = bwr.readings.filterNot(_.id == reading.id))
                    else bwr
                  }
                )
              case _ =>
                deletingVar.set(false)
            }
          },
        ),
      ),
    )
