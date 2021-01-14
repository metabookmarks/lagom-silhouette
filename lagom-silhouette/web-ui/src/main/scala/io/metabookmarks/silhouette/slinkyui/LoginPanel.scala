package io.metabookmarks.silhouette.slinkyui

import slinky.core.annotations.react
import slinky.core.Component
import io.metabookmarks.silhouette.Provider
import slinky.core.facade.ReactElement
import slinky.web.html._
import slinky.materialui.core._
import scalajs.js
import js.Dynamic.{literal => CSS}

import sttp.client3._

import io.circe._, io.circe.parser._
import io.circe.generic.auto._, io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import slinky.web.ReactDOM
import slinky.core.FunctionalComponent

@js.native
trait LoginPanelStyles extends js.Object {
  val root: String = js.native
  val paper: String = js.native
  val image: String = js.native
  val img: String = js.native
}

@react class LoginPanel extends Component {

  case class Props(origin: String)

  case class State(providers: List[Provider] = List.empty)

  def render(): ReactElement = ProvidersPanel(state.providers)

  def initialState: State = State()

  override def componentDidMount(): Unit = {
    implicit val sttpBackend = FetchBackend()

    basicRequest
      .get(uri"${props.origin}/auth/providers")
      .send()
      .foreach {
        case resp =>
          resp.body match {
            case Right(body) =>
              decode[List[Provider]](body) match {
                case Right(providers) =>
                  setState(st => st.copy(providers = providers))
                case Left(value) =>
                  println(s"ERROR: $value")
              }
            case Left(value) =>
              println(s"ERROR: $value")
          }
      }
  }
}

@react object ProvidersPanel {

  case class Props(providers: List[Provider])

  val useStyles = makeStyles[LoginPanelStyles]((theme: js.Object) =>
    CSS(
      "root" -> CSS(
        "flexGrow" -> 1
      ),
      "paper" -> CSS(
        "margin" -> "auto",
        "maxWidth" -> 500
      ),
      "image" -> CSS(
        "width" -> 128,
        "heigh" -> 128
      ),
      "img" -> CSS(
        "margin" -> "auto",
        "display" -> "block",
        "maxWidth" -> "48px",
        "maxHeight" -> "48px"
      )
    )
  )

  val component = FunctionalComponent[Props] { props =>
    val classes: LoginPanelStyles = useStyles()

    def providerPanel(provider: Provider) =
      GridItem(provider.name)(
        Button(classes = classes, onClick = e => println("o"))(href := s"/auth/authenticate/${provider.name}")(
          img(className := s"${classes.img} ${provider.name}", src := s"/auth/img/provider/${provider.name}.png")
        )
      )

    GridContainer(
      props.providers.map(providerPanel)
    )

  }

}
