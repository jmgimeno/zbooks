package zbooks.frontend.components

import com.raquo.laminar.api.L.*

object StarRating:

  /** Read-only star display */
  def display(evaluation: Option[Int]): HtmlElement =
    div(
      cls := "flex gap-0.5",
      (1 to 5).map { i =>
        span(
          cls := (if evaluation.exists(_ >= i) then "text-yellow-400 text-lg" else "text-gray-300 text-lg"),
          "★",
        )
      },
    )

  /** Interactive star input bound to a Var[Option[Int]] */
  def input(value: Var[Option[Int]]): HtmlElement =
    val hovered: Var[Option[Int]] = Var(None)

    div(
      cls := "flex gap-0.5 cursor-pointer",
      (1 to 5).map { i =>
        span(
          cls <-- hovered.signal.combineWith(value.signal).map { (h, v) =>
            val active = h.exists(_ >= i) || (h.isEmpty && v.exists(_ >= i))
            if active then "text-yellow-400 text-lg hover:text-yellow-500"
            else "text-gray-300 text-lg hover:text-yellow-400"
          },
          "★",
          onMouseEnter --> { _ => hovered.set(Some(i)) },
          onMouseLeave --> { _ => hovered.set(None) },
          onClick --> { _ =>
            // Toggle off if clicking the same value
            value.update(current => if current.contains(i) then None else Some(i))
          },
        )
      },
    )
