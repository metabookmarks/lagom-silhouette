package io.metabookmarks.silhouette

import org.scalajs.dom
import org.scalajs.dom
import org.scalajs.dom.document
import slinky.web.ReactDOM
import io.metabookmarks.silhouette.slinkyui.LoginPanel
object LoginSPA {

  def main(args: Array[String]): Unit =
    document.location.origin.foreach { origin =>
      Option(dom.document.getElementById("here")).foreach { container =>
        ReactDOM.render(LoginPanel(origin), container)

      }
    }

}
