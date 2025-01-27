package lila.perfStat

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.ui.Context
import lila.rating.PerfType
import lila.rating.GlickoExt.clueless
import lila.core.perm.Granter
import lila.core.i18n.{ I18nKey as trans, Translate }

final class PerfStatUi(i18nHelper: lila.ui.I18nHelper, dateHelper: lila.ui.DateHelper)(
    routeRoundWatcher: (String, String) => Call,
    userLink: UserId => Translate ?=> Frag,
    percentileText: (User, PerfType, Double) => Context ?=> Frag
):
  import lila.ui.NumberHelper.*
  import i18nHelper.given
  import dateHelper.*
  import trans.perfStat.*

  def ratingHistoryContainer = div(cls := "rating-history-container")(
    div(cls := "rating-history-container")(
      div(cls := "time-selector-buttons"),
      lila.ui.HtmlHelper.spinner,
      div(cls := "chart-container")(canvas(cls := "rating-history")),
      div(id := "time-range-slider")
    )
  )

  def content(data: PerfStatData)(using Context): Frag =
    import data.*
    div(cls := "box__pad perf-stat__content")(
      glicko(user.user, stat.perfType, user.perfs(stat.perfType), percentile),
      counter(stat.count),
      highlow(stat, percentileLow, percentileHigh),
      resultStreak(stat.resultStreak),
      result(stat, user.user),
      playStreakNb(stat.playStreak),
      playStreakTime(stat.playStreak)
    )

  private def decimal(v: Double) = scalalib.Maths.roundDownAt(v, 2)

  private def glicko(u: User, pt: PerfType, perf: Perf, percentile: Option[Double])(using Context): Frag =
    st.section(cls := "glicko")(
      h2(
        trans.site.perfRatingX(
          strong(
            if perf.glicko.clueless then "?"
            else decimal(perf.glicko.rating).toString
          )
        ),
        perf.glicko.provisional.yes.option(
          frag(
            " ",
            span(
              title := notEnoughRatedGames.txt(),
              cls   := "details"
            )("(", provisional(), ")")
          )
        ),
        ". ",
        percentile.filter(_ != 0.0 && perf.glicko.provisional.no).map { percentile =>
          span(cls := "details")(
            percentileText(u, pt, percentile)
          )
        }
      ),
      p(
        progressOverLastXGames(12),
        " ",
        span(cls := "progress")(
          if perf.progress > 0 then tag("green")(dataIcon := Icon.ArrowUpRight)(perf.progress)
          else if perf.progress < 0 then tag("red")(dataIcon := Icon.ArrowDownRight)(-perf.progress)
          else "-"
        ),
        ". ",
        ratingDeviation(
          strong(
            title := ratingDeviationTooltip.txt(
              lila.rating.Glicko.provisionalDeviation,
              lila.rating.Glicko.standardRankableDeviation,
              lila.rating.Glicko.variantRankableDeviation
            )
          )(decimal(perf.glicko.deviation).toString)
        )
      )
    )

  private def pct(num: Int, denom: Int): String =
    (denom != 0).so(s"${Math.round(num * 100.0 / denom)}%")

  private def counter(count: lila.perfStat.Count)(using Translate): Frag =
    st.section(cls := "counter split")(
      div(
        table(
          tbody(
            tr(
              th(totalGames()),
              td(count.all.localize),
              td
            ),
            tr(cls := "full")(
              th(ratedGames()),
              td(count.rated.localize),
              td(pct(count.rated, count.all))
            ),
            tr(cls := "full")(
              th(tournamentGames()),
              td(count.tour.localize),
              td(pct(count.tour, count.all))
            ),
            tr(cls := "full")(
              th(berserkedGames()),
              td(count.berserk.localize),
              td(pct(count.berserk, count.tour))
            ),
            (count.seconds > 0).option(
              tr(cls := "full")(
                th(timeSpentPlaying()),
                td(colspan := "2")(lila.core.i18n.translateDuration(count.duration))
              )
            )
          )
        )
      ),
      div(
        table(
          tbody(
            tr(
              th(averageOpponent()),
              td(decimal(count.opAvg.avg).toString),
              td
            ),
            tr(cls := "full")(
              th(victories()),
              td(tag("green")(count.win.localize)),
              td(tag("green")(pct(count.win, count.all)))
            ),
            tr(cls := "full")(
              th(trans.site.draws()),
              td(count.draw.localize),
              td(pct(count.draw, count.all))
            ),
            tr(cls := "full")(
              th(defeats()),
              td(tag("red")(count.loss.localize)),
              td(tag("red")(pct(count.loss, count.all)))
            ),
            tr(cls := "full")(
              th(disconnections()),
              td((count.disconnects > count.all * 100 / 15).option(tag("red")))(
                count.disconnects.localize
              ),
              td(pct(count.disconnects, count.all))
            )
          )
        )
      )
    )

  private def highlowSide(
      title: Frag => Frag,
      opt: Option[lila.perfStat.RatingAt],
      pctStr: Option[String],
      color: String
  )(using Translate): Frag = opt match
    case Some(r) =>
      div(
        h2(title(strong(tag(color)(r.int, pctStr.map(st.title := _))))),
        a(cls := "glpt", href := routeRoundWatcher(r.gameId, "white"))(absClientInstant(r.at))
      )
    case None => div(h2(title(emptyFrag)), " ", span(notEnoughGames()))

  private def highlow(stat: PerfStat, pctLow: Option[Double], pctHigh: Option[Double])(using
      Translate
  ): Frag =
    import stat.perfType
    def titleOf(v: Double) = trans.site.betterThanPercentPlayers.txt(s"$v%", perfType.trans)
    st.section(cls := "highlow split")(
      highlowSide(highestRating(_), stat.highest, pctHigh.map(titleOf), "green"),
      highlowSide(lowestRating(_), stat.lowest, pctLow.map(titleOf), "red")
    )

  private def fromTo(s: lila.perfStat.Streak)(using Translate): Frag =
    s.from match
      case Some(from) =>
        fromXToY(
          a(cls := "glpt", href := routeRoundWatcher(from.gameId, "white"))(absClientInstant(from.at)),
          s.to match
            case Some(to) =>
              a(cls := "glpt", href := routeRoundWatcher(to.gameId, "white"))(absClientInstant(to.at))
            case None => now()
        )
      case None => nbsp

  private def resultStreakSideStreak(s: lila.perfStat.Streak, title: Frag => Frag, color: String)(using
      Translate
  ): Frag =
    div(cls := "streak")(
      h3(
        title(
          if s.v > 0 then tag(color)(trans.site.nbGames.plural(s.v, strong(s.v.localize)))
          else "-"
        )
      ),
      fromTo(s)
    )

  private def resultStreakSide(s: lila.perfStat.Streaks, title: Frag, color: String)(using
      Translate
  ): Frag =
    div(
      h2(title),
      resultStreakSideStreak(s.max, longestStreak(_), color),
      resultStreakSideStreak(s.cur, currentStreak(_), color)
    )

  private def resultStreak(streak: lila.perfStat.ResultStreak)(using Translate): Frag =
    st.section(cls := "resultStreak split")(
      resultStreakSide(streak.win, winningStreak(), "green"),
      resultStreakSide(streak.loss, losingStreak(), "red")
    )

  private def resultTable(results: lila.perfStat.Results, title: Frag, user: User)(using Translate) =
    div:
      table(
        thead:
          tr(th(colspan := 2)(h2(title)))
        ,
        tbody:
          results.results.map: r =>
            tr(
              td(userLink(r.opId), " (", r.opRating, ")"),
              td:
                a(cls := "glpt", href := s"${routeRoundWatcher(r.gameId, "white")}?pov=${user.username}"):
                  absClientInstant(r.at)
            )
      )

  private def result(stat: PerfStat, user: User)(using Context): Frag =
    st.section(cls := "result split")(
      resultTable(stat.bestWins, bestRated(), user),
      (Granter.opt(_.BoostHunter) || Granter.opt(_.CheatHunter)).option(
        resultTable(
          stat.worstLosses,
          "Worst rated defeats",
          user
        )
      )
    )

  private def playStreakNbStreak(s: lila.perfStat.Streak, title: Frag => Frag)(using Translate): Frag =
    div(
      div(cls := "streak")(
        h3(
          title(
            if s.v > 0 then trans.site.nbGames.plural(s.v, strong(s.v.localize))
            else "-"
          )
        ),
        fromTo(s)
      )
    )

  private def playStreakNbStreaks(streaks: lila.perfStat.Streaks)(using Translate): Frag =
    div(cls := "split")(
      playStreakNbStreak(streaks.max, longestStreak(_)),
      playStreakNbStreak(streaks.cur, currentStreak(_))
    )

  private def playStreakNb(playStreak: lila.perfStat.PlayStreak)(using Translate): Frag =
    st.section(cls := "playStreak")(
      h2(span(title := lessThanOneHour.txt())(gamesInARow())),
      playStreakNbStreaks(playStreak.nb)
    )

  private def playStreakTimeStreak(s: lila.perfStat.Streak, title: Frag => Frag)(using Translate): Frag =
    div(
      div(cls := "streak")(
        h3(title(lila.core.i18n.translateDuration(s.duration))),
        fromTo(s)
      )
    )

  private def playStreakTimeStreaks(streaks: lila.perfStat.Streaks)(using Translate): Frag =
    div(cls := "split")(
      playStreakTimeStreak(streaks.max, longestStreak(_)),
      playStreakTimeStreak(streaks.cur, currentStreak(_))
    )

  private def playStreakTime(playStreak: lila.perfStat.PlayStreak)(using Translate): Frag =
    st.section(cls := "playStreak")(
      h2(span(title := lessThanOneHour.txt())(maxTimePlaying())),
      playStreakTimeStreaks(playStreak.time)
    )
