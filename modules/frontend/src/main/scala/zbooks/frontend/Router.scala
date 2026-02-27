package zbooks.frontend

import com.raquo.laminar.api.L.*
import org.scalajs.dom

sealed trait Page
case object BookList                 extends Page
case class  BookDetail(id: Long)     extends Page

object Router:

  val currentPage: Var[Page] = Var(parsePage(dom.window.location.hash))

  def navigate(page: Page): Unit =
    val hash = page match
      case BookList        => "#/"
      case BookDetail(id)  => s"#/books/$id"
    dom.window.location.hash = hash

  def init(): Unit =
    dom.window.addEventListener("hashchange", (_: dom.Event) => {
      currentPage.set(parsePage(dom.window.location.hash))
    })

  private def parsePage(hash: String): Page =
    val clean = hash.stripPrefix("#").stripPrefix("/")
    clean match
      case s if s.startsWith("books/") =>
        s.stripPrefix("books/").toLongOption.map(BookDetail(_)).getOrElse(BookList)
      case _ => BookList
