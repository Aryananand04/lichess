package views.html.game

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.*

import lila.rating.PerfType
import lila.game.GameExt.perfType

lazy val ui         = lila.game.ui.GameUi(i18nHelper, dateHelper)(routeRoundWatcher = routes.Round.watcher)
lazy val crosstable = ui.crosstable(userId => _ ?=> userIdLink(userId.some, withOnline = false))

object bits:

  def sides(
      pov: Pov,
      initialFen: Option[chess.format.Fen.Full],
      tour: Option[lila.tournament.TourAndTeamVs],
      cross: Option[lila.game.Crosstable.WithMatchup],
      simul: Option[lila.simul.Simul],
      userTv: Option[User] = None,
      bookmarked: Boolean
  )(using ctx: Context) =
    div(
      side.meta(pov, initialFen, tour, simul, userTv, bookmarked = bookmarked),
      cross.map: c =>
        div(cls := "crosstable")(crosstable(ctx.userId.fold(c)(c.fromPov), pov.gameId.some))
    )

  def variantLink(
      variant: chess.variant.Variant,
      perfType: PerfType,
      initialFen: Option[chess.format.Fen.Full] = None,
      shortName: Boolean = false
  )(using Translate): Frag =

    def link(href: String, title: String, name: String) = a(
      cls     := "variant-link",
      st.href := href,
      targetBlank,
      st.title := title
    )(name)

    if variant.exotic then
      link(
        href = variant match
          case chess.variant.FromPosition =>
            s"""${routes.Editor.index}?fen=${initialFen.so(_.value.replace(' ', '_'))}"""
          case v => routes.Cms.variant(v.key).url
        ,
        title = variant.title,
        name = (if shortName && variant == chess.variant.KingOfTheHill then variant.shortName
                else variant.name).toUpperCase
      )
    else if perfType == PerfType.Correspondence then
      link(
        href = s"${routes.Main.faq}#correspondence",
        title = PerfType.Correspondence.desc,
        name = PerfType.Correspondence.trans
      )
    else span(title := perfType.desc)(perfType.trans)
