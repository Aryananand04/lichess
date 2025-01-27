package views.html.tournament

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.tournament.TournamentShield

object shields:

  private val section = st.section(cls := "tournament-shields__item")

  def apply(history: TournamentShield.History)(using PageContext) =
    views.html.base.layout(
      title = "Tournament shields",
      moreCss = cssTag("tournament.leaderboard"),
      wrapClass = "full-screen-force"
    ) {
      main(cls := "page-menu")(
        views.html.user.bits.communityMenu("shield"),
        div(cls := "page-menu__content box box-pad")(
          h1(cls := "box__top")(trans.arena.tournamentShields()),
          div(cls := "tournament-shields")(
            history.sorted.map { (categ, awards) =>
              section(
                h2(
                  a(href := routes.Tournament.categShields(categ.key))(
                    span(cls := "shield-trophy")(categ.icon),
                    categ.name
                  )
                ),
                ol(awards.map { aw =>
                  li(
                    userIdLink(aw.owner.some),
                    a(href := routes.Tournament.show(aw.tourId))(showDate(aw.date))
                  )
                })
              )
            }
          )
        )
      )
    }

  def byCateg(categ: TournamentShield.Category, awards: List[TournamentShield.Award])(using PageContext) =
    views.html.base.layout(
      title = "Tournament shields",
      moreCss = frag(cssTag("tournament.leaderboard"), cssTag("slist"))
    ) {
      main(cls := "page-menu page-small tournament-categ-shields")(
        views.html.user.bits.communityMenu("shield"),
        div(cls := "page-menu__content box")(
          boxTop(
            h1(
              a(href := routes.Tournament.shields, dataIcon := Icon.LessThan, cls := "text"),
              frag(categ.name, " • ", trans.arena.tournamentShields())
            )
          ),
          ol(awards.map { aw =>
            li(
              span(cls := "shield-trophy")(categ.icon),
              userIdLink(aw.owner.some),
              a(href := routes.Tournament.show(aw.tourId))(showDate(aw.date))
            )
          })
        )
      )
    }
