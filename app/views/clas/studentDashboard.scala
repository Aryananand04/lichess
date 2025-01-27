package views.html.clas

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.clas.{ Clas, Student }
import lila.common.String.html.richText
import lila.rating.UserPerfsExt.bestAny3Perfs
import lila.rating.UserPerfsExt.bestRating

object studentDashboard:

  def apply(
      c: Clas,
      wall: Html,
      teachers: List[User],
      students: List[Student.WithUserPerfs]
  )(using PageContext) =
    bits.layout(c.name, Left(c.withStudents(Nil)))(
      cls := "clas-show dashboard dashboard-student",
      div(cls := "clas-show__top")(
        h1(dataIcon := Icon.Group, cls := "text")(c.name),
        c.desc.trim.nonEmpty.option(div(cls := "clas-show__desc")(richText(c.desc)))
      ),
      c.archived.map { archived =>
        div(cls := "box__pad")(
          div(cls := "clas-show__archived archived")(bits.showArchived(archived))
        )
      },
      table(cls := "slist slist-pad teachers")(
        thead:
          tr(
            th(trans.clas.nbTeachers(teachers.size)),
            th,
            th
          )
        ,
        tbody:
          teachers.map: user =>
            tr(
              td(
                userLink(
                  user,
                  name = span(
                    strong(user.username),
                    user.profile.flatMap(_.nonEmptyRealName).map { em(_) }
                  ).some,
                  withTitle = false
                )
              ),
              td(
                user.seenAt.map: seen =>
                  trans.site.lastSeenActive(momentFromNow(seen))
              ),
              challengeTd(user)
            )
      ),
      c.wall.value.nonEmpty.option(div(cls := "box__pad clas-wall")(rawHtml(wall))),
      div(cls := "students")(studentList(students))
    )

  def studentList(students: List[Student.WithUserPerfs])(using PageContext) =
    table(cls := "slist slist-pad sortable")(
      thead:
        tr(
          th(dataSortDefault)(trans.clas.nbStudents(students.size)),
          thSortNumber(trans.site.rating()),
          thSortNumber(trans.site.games()),
          thSortNumber(trans.site.puzzles()),
          th
        )
      ,
      tbody:
        students.sortBy(-_.user.seenAt.so(_.toMillis)).map {
          case Student.WithUserPerfs(student, user, perfs) =>
            tr(
              td(
                userLink(
                  user,
                  name = span(
                    strong(user.username),
                    em(student.realName)
                  ).some,
                  withTitle = false
                )
              ),
              td(dataSort := perfs.bestRating, cls := "rating")(cls := "rating")(perfs.bestAny3Perfs.map:
                showPerfRating(perfs, _)
              ),
              td(user.count.game.localize),
              td(perfs.puzzle.nb.localize),
              challengeTd(user)
            )
        }
    )

  private def challengeTd(user: User)(using ctx: PageContext) =
    if ctx.is(user) then td
    else
      val online = isOnline(user.id)
      td(
        a(
          dataIcon := Icon.Swords,
          cls      := List("button button-empty text" -> true, "disabled" -> !online),
          title    := trans.challenge.challengeToPlay.txt(),
          href     := online.option(s"${routes.Lobby.home}?user=${user.username}#friend")
        )(trans.site.play())
      )
