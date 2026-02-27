package zbooks.frontend

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import zbooks.frontend.components.App

object Main:

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(
      dom.document.getElementById("app"),
      {
        Router.init()
        App.loadInitialData()
        App()
      },
    )
