package zbooks.frontend.components

import com.raquo.laminar.api.L.*
import zbooks.frontend.api.ApiClient
import zbooks.frontend.state.AppState
import zbooks.shared.api.{CreateReadingRequest, UpdateReadingRequest}
import zbooks.shared.models.Reading

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object ReadingForm:

  def addForm(bookId: Long, onDone: () => Unit): HtmlElement =
    val startVar  = Var("")
    val endVar    = Var("")
    val savingVar = Var(false)
    val errVar    = Var(Option.empty[String])

    form(
      cls := "bg-gray-50 rounded-xl p-4 space-y-3 border border-gray-200",
      onSubmit.preventDefault --> { _ =>
        savingVar.set(true)
        errVar.set(None)
        ApiClient.createReading(bookId, CreateReadingRequest(startVar.now(), endVar.now())).onComplete {
          case Success(Right(reading)) =>
            AppState.booksVar.update(books =>
              books.map(bwr =>
                if bwr.book.id == bookId then bwr.copy(readings = bwr.readings :+ reading)
                else bwr
              )
            )
            savingVar.set(false)
            onDone()
          case Success(Left(err)) =>
            errVar.set(Some(err))
            savingVar.set(false)
          case Failure(ex) =>
            errVar.set(Some(ex.getMessage))
            savingVar.set(false)
        }
      },
      h4(cls := "text-sm font-medium text-gray-700", "Add Reading Session"),
      dateRow(startVar, endVar),
      child.maybe <-- errVar.signal.map(_.map(e => p(cls := "text-sm text-red-600", e))),
      div(
        cls := "flex gap-2 justify-end",
        button(
          tpe := "button",
          cls := "px-3 py-1.5 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-100 transition-colors",
          "Cancel",
          onClick --> { _ => onDone() },
        ),
        button(
          tpe      := "submit",
          cls      := "px-3 py-1.5 text-sm rounded-lg bg-green-600 text-white hover:bg-green-700 transition-colors disabled:opacity-50",
          disabled <-- savingVar.signal,
          child.text <-- savingVar.signal.map(s => if s then "Saving…" else "Save"),
        ),
      ),
    )

  def editForm(bookId: Long, reading: Reading, onDone: () => Unit): HtmlElement =
    val startVar  = Var(reading.startDate)
    val endVar    = Var(reading.endDate)
    val savingVar = Var(false)
    val errVar    = Var(Option.empty[String])

    form(
      cls := "bg-gray-50 rounded-xl p-4 space-y-3 border border-blue-200",
      onSubmit.preventDefault --> { _ =>
        savingVar.set(true)
        errVar.set(None)
        ApiClient.updateReading(bookId, reading.id, UpdateReadingRequest(startVar.now(), endVar.now())).onComplete {
          case Success(Right(updated)) =>
            AppState.booksVar.update(books =>
              books.map(bwr =>
                if bwr.book.id == bookId then
                  bwr.copy(readings = bwr.readings.map(r => if r.id == reading.id then updated else r))
                else bwr
              )
            )
            savingVar.set(false)
            onDone()
          case Success(Left(err)) =>
            errVar.set(Some(err))
            savingVar.set(false)
          case Failure(ex) =>
            errVar.set(Some(ex.getMessage))
            savingVar.set(false)
        }
      },
      h4(cls := "text-sm font-medium text-gray-700", "Edit Reading Session"),
      dateRow(startVar, endVar),
      child.maybe <-- errVar.signal.map(_.map(e => p(cls := "text-sm text-red-600", e))),
      div(
        cls := "flex gap-2 justify-end",
        button(
          tpe := "button",
          cls := "px-3 py-1.5 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-100 transition-colors",
          "Cancel",
          onClick --> { _ => onDone() },
        ),
        button(
          tpe      := "submit",
          cls      := "px-3 py-1.5 text-sm rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors disabled:opacity-50",
          disabled <-- savingVar.signal,
          child.text <-- savingVar.signal.map(s => if s then "Saving…" else "Update"),
        ),
      ),
    )

  private def dateRow(startVar: Var[String], endVar: Var[String]): HtmlElement =
    div(
      cls := "grid grid-cols-2 gap-3",
      div(
        label(cls := "block text-xs text-gray-500 mb-1", "Start date"),
        input(
          tpe   := "date",
          cls   := "w-full px-2 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500",
          value <-- startVar.signal,
          onInput.mapToValue --> startVar.writer,
        ),
      ),
      div(
        label(cls := "block text-xs text-gray-500 mb-1", "End date"),
        input(
          tpe   := "date",
          cls   := "w-full px-2 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500",
          value <-- endVar.signal,
          onInput.mapToValue --> endVar.writer,
        ),
      ),
    )
