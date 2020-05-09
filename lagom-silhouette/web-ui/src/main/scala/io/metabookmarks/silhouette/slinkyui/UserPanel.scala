package io.metabookmarks.silhouette.slinkyui

import slinky.core.annotations.react
import slinky.core.StatelessComponent
import slinky.core.facade.ReactElement

import slinky.web.html._
import slinky.materialui.core._
import scalajs.js
import scala.scalajs.js.annotation.JSImport

import slinky.core.FunctionalComponent

import js.Dynamic.{literal => CSS}
import io.metabookmarks.silhouette.User
import io.metabookmarks.silhouette.Profile

@js.native
trait Styles extends js.Object {
  val root: String = js.native
  val paper: String = js.native
  val image: String = js.native
  val img: String = js.native
}

@react object UserPanel {
  case class Props(user: User)
  val useStyles = makeStyles[Styles](
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

  val component = FunctionalComponent[Props] { props =>
    val classes: Styles = useStyles()

    def userPanel(user: User) =
      Paper(className = classes.paper)(
        GridContainer(spacing = 2)(
          GridItem(key = s"photo")(
            ButtonBase(className = classes.image)(
              img(className := classes.img, src := user.avatarURL)
            )
          ),
          Grid(key = s"gen", sm = true, xs = Size.`12`)(
            Grid(xs = true, direction = "column", spacing = 2)(
              GridItem(xs = true)(
                Typography(gutterBottom = true, variant = "subtitle1")(user.email),
                Typography(user.firstName),
                Typography(user.lastName)
              )
            )
          )
        )
      )

    def profilePanel(profile: Profile) =
      Paper(className = classes.paper)(
        GridContainer(spacing = 2)(
          GridItem(key = s"photo")(
            ButtonBase(className = classes.image)(
              img(className := classes.img, src := profile.avatarURL)
            )
          ),
          Grid(key = s"gen", sm = true, xs = Size.`12`)(
            Grid(xs = true, direction = "column", spacing = 2)(
              GridItem(xs = true)(
                Typography(gutterBottom = true, variant = "subtitle1")(profile.providerKey),
                Typography(profile.fullName)
              )
            )
          )
        )
      )

    div(className := classes.root)(
      userPanel(props.user),
      props.user.profiles.map {
        case (provider, profile) =>
          profilePanel(profile)
      }
    )
  }
}
