package views.html
package round

import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.round.RoundGame.secondsSinceCreation

object watcher:

  def apply(
      pov: Pov,
      data: JsObject,
      tour: Option[lila.tournament.TourAndTeamVs],
      simul: Option[lila.simul.Simul],
      cross: Option[lila.game.Crosstable.WithMatchup],
      userTv: Option[User] = None,
      chatOption: Option[lila.chat.UserChat.Mine],
      bookmarked: Boolean
  )(using ctx: PageContext) =

    val chatJson = chatOption.map: c =>
      chat.json(
        c.chat,
        c.lines,
        name = trans.site.spectatorRoom.txt(),
        timeout = c.timeout,
        withNoteAge = ctx.isAuth.option(pov.game.secondsSinceCreation),
        public = true,
        resourceId = lila.chat.Chat.ResourceId(s"game/${c.chat.id}"),
        palantir = ctx.canPalantir
      )

    bits.layout(
      variant = pov.game.variant,
      title = s"${gameVsText(pov.game, withRatings = ctx.pref.showRatings)} • spectator",
      modules = roundNvuiTag,
      pageModule = PageModule(
        "round",
        Json.obj(
          "data" -> data,
          "i18n" -> jsI18n(pov.game),
          "chat" -> chatJson
        )
      ).some,
      openGraph = povOpenGraph(pov).some,
      zenable = true
    ):
      main(cls := "round")(
        st.aside(cls := "round__side")(
          bits.side(pov, data, tour, simul, userTv, bookmarked),
          chatOption.map(_ => chat.frag)
        ),
        bits.roundAppPreload(pov),
        div(cls := "round__underboard")(bits.crosstable(cross, pov.game)),
        div(cls := "round__underchat")(bits.underchat(pov.game))
      )

  def crawler(pov: Pov, initialFen: Option[chess.format.Fen.Full], pgn: chess.format.pgn.Pgn)(using
      ctx: PageContext
  ) =
    bits.layout(
      variant = pov.game.variant,
      title = gameVsText(pov.game, withRatings = true),
      openGraph = povOpenGraph(pov).some,
      pageModule = none
    ):
      main(cls := "round")(
        st.aside(cls := "round__side")(
          game.side(pov, initialFen, none, simul = none, userTv = none, bookmarked = false),
          div(cls := "for-crawler")(
            h1(titleGame(pov.game)),
            p(describePov(pov)),
            div(cls := "pgn")(pgn.render)
          )
        ),
        div(cls := "round__board main-board")(chessground(pov))
      )
