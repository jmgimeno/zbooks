package zbooks.frontend.components

import com.raquo.laminar.api.L.*
import zbooks.frontend.api.ApiClient
import zbooks.frontend.state.AppState
import zbooks.shared.api.{BookWithReadings, CreateBookRequest, UpdateBookRequest}
import zbooks.shared.models.Book

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object BookForm:

  def addModal(onClose: () => Unit): HtmlElement =
    val nameVar       = Var("")
    val authorVar     = Var("")
    val editorVar     = Var("")
    val yearVar       = Var("")
    val evaluationVar = Var(Option.empty[Int])
    val savingVar     = Var(false)
    val formErrorVar  = Var(Option.empty[String])

    modal(
      title   = "Add Book",
      onClose = onClose,
      onSave  = () =>
        yearVar.now().toIntOption match
          case None =>
            formErrorVar.set(Some("Year must be a number"))
          case Some(year) =>
            savingVar.set(true)
            val req = CreateBookRequest(
              name       = nameVar.now().trim,
              author     = authorVar.now().trim,
              editor     = editorVar.now().trim,
              year       = year,
              evaluation = evaluationVar.now(),
            )
            ApiClient.createBook(req).onComplete {
              case Success(Right(book)) =>
                AppState.booksVar.update(_ :+ BookWithReadings(book, Nil))
                savingVar.set(false)
                onClose()
              case Success(Left(err)) =>
                formErrorVar.set(Some(err))
                savingVar.set(false)
              case Failure(ex) =>
                formErrorVar.set(Some(ex.getMessage))
                savingVar.set(false)
            },
      fields       = formFields(nameVar, authorVar, editorVar, yearVar, evaluationVar),
      savingSignal = savingVar.signal,
      errorSignal  = formErrorVar.signal,
    )

  def editModal(bwr: BookWithReadings, onClose: () => Unit): HtmlElement =
    val b             = bwr.book
    val nameVar       = Var(b.name)
    val authorVar     = Var(b.author)
    val editorVar     = Var(b.editor)
    val yearVar       = Var(b.year.toString)
    val evaluationVar = Var(b.evaluation)
    val savingVar     = Var(false)
    val formErrorVar  = Var(Option.empty[String])

    modal(
      title   = "Edit Book",
      onClose = onClose,
      onSave  = () =>
        yearVar.now().toIntOption match
          case None =>
            formErrorVar.set(Some("Year must be a number"))
          case Some(year) =>
            savingVar.set(true)
            val req = UpdateBookRequest(
              name       = nameVar.now().trim,
              author     = authorVar.now().trim,
              editor     = editorVar.now().trim,
              year       = year,
              evaluation = evaluationVar.now(),
            )
            ApiClient.updateBook(b.id, req).onComplete {
              case Success(Right(updated)) =>
                AppState.booksVar.update(books =>
                  books.map(item => if item.book.id == b.id then item.copy(book = updated) else item)
                )
                savingVar.set(false)
                onClose()
              case Success(Left(err)) =>
                formErrorVar.set(Some(err))
                savingVar.set(false)
              case Failure(ex) =>
                formErrorVar.set(Some(ex.getMessage))
                savingVar.set(false)
            },
      fields       = formFields(nameVar, authorVar, editorVar, yearVar, evaluationVar),
      savingSignal = savingVar.signal,
      errorSignal  = formErrorVar.signal,
    )

  private def formFields(
      nameVar: Var[String],
      authorVar: Var[String],
      editorVar: Var[String],
      yearVar: Var[String],
      evaluationVar: Var[Option[Int]],
  ): HtmlElement =
    div(
      cls := "space-y-4",
      labeledInput("Title", "text", nameVar, "e.g. The Pragmatic Programmer"),
      labeledInput("Author", "text", authorVar, "e.g. David Thomas"),
      labeledInput("Editor / Publisher", "text", editorVar, "e.g. Addison-Wesley"),
      labeledInput("Year", "number", yearVar, "e.g. 1999"),
      div(
        label(cls := "block text-sm font-medium text-gray-700 mb-1", "Rating (optional)"),
        StarRating.input(evaluationVar),
      ),
    )

  private def labeledInput(
      labelText: String,
      inputType: String,
      binder: Var[String],
      hint: String,
  ): HtmlElement =
    div(
      label(cls := "block text-sm font-medium text-gray-700 mb-1", labelText),
      input(
        tpe         := inputType,
        cls         := "w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500",
        placeholder := hint,
        value       <-- binder.signal,
        onInput.mapToValue --> binder.writer,
      ),
    )

  private def modal(
      title: String,
      onClose: () => Unit,
      onSave: () => Unit,
      fields: HtmlElement,
      savingSignal: Signal[Boolean],
      errorSignal: Signal[Option[String]],
  ): HtmlElement =
    div(
      cls := "fixed inset-0 z-50 flex items-center justify-center bg-black/40",
      onClick --> { e =>
        if e.target == e.currentTarget then onClose()
      },
      div(
        cls := "bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6",
        div(
          cls := "flex items-center justify-between mb-6",
          h2(cls := "text-xl font-semibold text-gray-900", title),
          button(
            tpe := "button",
            cls := "text-gray-400 hover:text-gray-600 transition-colors",
            "✕",
            onClick --> { _ => onClose() },
          ),
        ),
        fields,
        child.maybe <-- errorSignal.map(_.map(err =>
          p(cls := "mt-3 text-sm text-red-600", err)
        )),
        div(
          cls := "mt-6 flex gap-3 justify-end",
          button(
            tpe := "button",
            cls := "px-4 py-2 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors",
            "Cancel",
            onClick --> { _ => onClose() },
          ),
          button(
            tpe      := "button",
            cls      := "px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors disabled:opacity-50",
            disabled <-- savingSignal,
            child.text <-- savingSignal.map(s => if s then "Saving…" else "Save"),
            onClick --> { _ => onSave() },
          ),
        ),
      ),
    )
