package views.html
package tournament

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator
import lila.tournament.Tournament
import lila.tournament.crud.CrudForm
import lila.tournament.ui.{ FormPrefix, TourFields }

object crud:
  given prefix: FormPrefix = FormPrefix.make("setup")

  private def layout(
      title: String,
      modules: EsmList = Nil,
      evenMoreJs: Frag = emptyFrag,
      css: String = "mod.misc"
  )(body: Frag)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag(css),
      modules = jsModule("bits.flatpick") ++ modules,
      moreJs = evenMoreJs
    ):
      main(cls := "page-menu")(
        views.html.mod.menu("tour"),
        body
      )

  def create(form: Form[?])(using PageContext) =
    layout(
      title = "New tournament",
      css = "mod.form"
    ):
      div(cls := "crud page-menu__content box box-pad")(
        h1(cls := "box__top")("New tournament"),
        postForm(cls := "form3", action := routes.TournamentCrud.create)(
          tournament.form.ui.spotlightAndTeamBattle(form, none),
          errMsg(form("setup")),
          tournament.form.ui.setupCreate(form, Nil),
          form3.action(form3.submit(trans.site.apply()))
        )
      )

  def edit(tour: Tournament, form: Form[?])(using PageContext) =
    layout(
      title = tour.name(),
      css = "mod.form"
    ):
      div(cls := "crud edit page-menu__content box box-pad")(
        boxTop(
          h1(
            a(href := routes.Tournament.show(tour.id))(tour.name()),
            " ",
            span("Created by ", titleNameOrId(tour.createdBy), " on ", showDate(tour.createdAt))
          ),
          st.form(
            cls    := "box__top__actions",
            action := routes.TournamentCrud.cloneT(tour.id),
            method := "get"
          )(form3.submit("Clone", Icon.Trophy.some)(cls := "button-green button-empty"))
        ),
        standardFlash,
        postForm(cls := "form3", action := routes.TournamentCrud.update(tour.id))(
          tournament.form.ui.spotlightAndTeamBattle(form, tour.some),
          errMsg(form("setup")),
          tournament.form.ui.setupEdit(tour, form, Nil),
          form3.action(form3.submit(trans.site.apply()))
        )
      )

  def index(tours: Paginator[Tournament])(using PageContext) =
    layout(
      title = "Tournament manager",
      modules = infiniteScrollTag
    ):
      div(cls := "crud page-menu__content box")(
        boxTop(
          h1("Tournament manager"),
          div(cls := "box__top__actions")(
            a(cls := "button button-green", href := routes.TournamentCrud.form, dataIcon := Icon.PlusButton)
          )
        ),
        table(cls := "slist slist-pad")(
          thead(
            tr(
              th(),
              th("Variant"),
              th("Clock"),
              th("Duration"),
              th(utcLink, " Date"),
              th()
            )
          ),
          tbody(cls := "infinite-scroll")(
            tours.currentPageResults.map: tour =>
              tr(cls := "paginated")(
                td(
                  a(href := routes.TournamentCrud.edit(tour.id))(
                    strong(tour.name()),
                    " ",
                    em(tour.spotlight.map(_.headline))
                  )
                ),
                td(tour.variant.name),
                td(tour.clock.toString),
                td(tour.minutes, "m"),
                td(showInstant(tour.startsAt), " ", momentFromNow(tour.startsAt, alwaysRelative = true)),
                td(a(href := routes.Tournament.show(tour.id), dataIcon := Icon.Eye, title := "View on site"))
              ),
            pagerNextTable(tours, routes.TournamentCrud.index(_).url)
          )
        )
      )
