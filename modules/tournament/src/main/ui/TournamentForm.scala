package lila.tournament
package ui

import play.api.data.*

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.ui.{ Context, FormHelper, I18nHelper }
import lila.core.i18n.{ Translate, I18nKey as trans }
import lila.gathering.{ ConditionForm, GatheringClock }
import lila.core.team.LightTeam
import lila.core.perm.Granter

opaque type FormPrefix = Option[String]
object FormPrefix extends TotalWrapper[FormPrefix, Option[String]]:
  val empty           = FormPrefix(none)
  def make(s: String) = FormPrefix(s.some)

extension (f: Form[?])
  def prefix(name: String)(using prefixOpt: FormPrefix) = f(
    prefixOpt.fold(name)(prefix => s"$prefix.$name")
  )

final class TournamentForm(
    val formHelper: FormHelper,
    val i18nHelper: I18nHelper,
    val translatedVariantChoicesWithVariants: Translate ?=> List[(String, String, Option[String])]
):
  import formHelper.*
  import i18nHelper.given

  def tourFields(form: Form[?], tour: Option[Tournament])(using Context, FormPrefix) =
    TourFields(this)(form, tour)

  def setupCreate(form: Form[?], leaderTeams: List[LightTeam])(using Context, FormPrefix) =
    val fields = tourFields(form, none)
    frag(
      form3.globalError(form),
      fields.name,
      form3.split(fields.rated, fields.variant),
      fields.clock,
      form3.split(fields.minutes, fields.waitMinutes),
      form3.split(fields.description(true), fields.startPosition),
      form3.fieldset(trans.site.advancedSettings())(cls := "conditions")(
        fields.advancedSettings,
        div(cls := "form")(
          conditionFields(form, fields, teams = leaderTeams, tour = none),
          fields.startDate
        )
      ),
      fields.isTeamBattle.option(form3.hidden(form.prefix("teamBattleByTeam")))
    )

  def setupEdit(tour: Tournament, form: Form[?], myTeams: List[LightTeam])(using Context, FormPrefix) =
    val fields = tourFields(form, tour.some)
    frag(
      form3.split(fields.name, tour.isCreated.option(fields.startDate)),
      form3.split(fields.rated, fields.variant),
      fields.clock,
      form3.split(
        if lila.tournament.TournamentForm.minutes contains tour.minutes then form3.split(fields.minutes)
        else
          form3.group(form.prefix("minutes"), trans.site.duration(), half = true):
            form3.input(_)(tpe := "number")
      ),
      form3.split(fields.description(true), fields.startPosition),
      form3.globalError(form),
      form3.fieldset(trans.site.advancedSettings())(cls := "conditions")(
        fields.advancedSettings,
        div(cls := "form"):
          conditionFields(form, fields, teams = myTeams, tour = tour.some)
      )
    )

  def conditionFields(
      form: Form[?],
      fields: TourFields,
      teams: List[LightTeam],
      tour: Option[Tournament]
  )(using ctx: Context)(using FormPrefix) =
    frag(
      form3.split(
        fields.entryCode,
        (tour.isEmpty && teams.nonEmpty).option {
          val baseField = form.prefix("conditions.teamMember.teamId")
          val field = ctx.req.queryString
            .get("team")
            .flatMap(_.headOption)
            .foldLeft(baseField): (field, team) =>
              field.copy(value = team.some)
          form3.group(field, trans.site.onlyMembersOfTeam(), half = true):
            form3.select(_, List(("", trans.site.noRestriction.txt())) ::: teams.map(_.pair))
        }
      ),
      form3.split(
        form3.group(form.prefix("conditions.nbRatedGame.nb"), trans.site.minimumRatedGames(), half = true):
          form3.select(_, ConditionForm.nbRatedGameChoices)
      ),
      form3.split(
        form3.group(form.prefix("conditions.minRating.rating"), trans.site.minimumRating(), half = true):
          form3.select(_, ConditionForm.minRatingChoices)
        ,
        form3
          .group(form.prefix("conditions.maxRating.rating"), trans.site.maximumWeeklyRating(), half = true):
            form3.select(_, ConditionForm.maxRatingChoices)
      ),
      form3.split(
        form3.group(
          form.prefix("conditions.allowList"),
          trans.swiss.predefinedUsers(),
          help = trans.swiss.forbiddedUsers().some,
          half = true
        )(form3.textarea(_)(rows := 4))
      ),
      form3.split(
        (ctx.me.exists(_.hasTitle) || Granter.opt(_.ManageTournament)).so {
          form3.checkbox(
            form.prefix("conditions.titled"),
            trans.arena.onlyTitled(),
            help = trans.arena.onlyTitledHelp().some,
            half = true
          )
        },
        form3.checkbox(
          form.prefix("berserkable"),
          trans.arena.allowBerserk(),
          help = trans.arena.allowBerserkHelp().some,
          half = true
        ),
        form3.hiddenFalse(form.prefix("berserkable"))
      ),
      form3.split(
        form3.checkbox(
          form.prefix("hasChat"),
          trans.site.chatRoom(),
          help = trans.arena.allowChatHelp().some,
          half = true
        ),
        form3.hiddenFalse(form.prefix("hasChat")),
        form3.checkbox(
          form.prefix("streakable"),
          trans.arena.arenaStreaks(),
          help = trans.arena.arenaStreaksHelp().some,
          half = true
        ),
        form3.hiddenFalse(form.prefix("streakable"))
      )
    )

  def spotlightAndTeamBattle(form: Form[?], tour: Option[Tournament])(using Context) =
    frag(
      form3.split(
        form3.group(
          form("homepageHours"),
          raw(s"Hours on homepage (0 to ${crud.CrudForm.maxHomepageHours})"),
          half = true,
          help = raw("Ask on zulip first").some
        )(form3.input(_, typ = "number")),
        form3.group(form("image"), raw("Custom icon"), half = true)(
          form3.select(_, crud.CrudForm.imageChoices)
        )
      ),
      form3.split(
        form3.group(
          form("headline"),
          raw("Homepage headline"),
          help = raw("Keep it VERY short, so it fits on homepage").some,
          half = true
        )(form3.input(_)),
        form3.group(
          form("id"),
          raw("Tournament ID (in the URL)"),
          help =
            raw("An 8-letter unique tournament ID, can't be changed after the tournament is created.").some,
          half = true
        )(f => form3.input(f)(tour.isDefined.option(readonly := true)))
      ),
      form3.checkbox(
        form("teamBattle"),
        raw("Team battle"),
        half = true
      )
    )

final class TourFields(tourForm: TournamentForm)(form: Form[?], tour: Option[Tournament])(using
    Context,
    FormPrefix
):
  import tourForm.*
  import i18nHelper.given
  import formHelper.*

  def isTeamBattle = tour.exists(_.isTeamBattle) || form.prefix("teamBattleByTeam").value.nonEmpty

  private def disabledAfterStart = tour.exists(!_.isCreated)

  def name =
    form3.group(form.prefix("name"), trans.site.name()) { f =>
      div(
        form3.input(f),
        " ",
        if isTeamBattle then trans.team.teamBattle() else trans.arena.arena(),
        br,
        small(cls := "form-help")(
          trans.site.safeTournamentName(),
          br,
          trans.site.inappropriateNameWarning(),
          br,
          trans.site.emptyTournamentName()
        )
      )
    }

  def rated =
    frag(
      form3.checkbox(
        form.prefix("rated"),
        trans.site.rated(),
        help = trans.site.ratedFormHelp().some
      ),
      form3.hiddenFalse(form.prefix("rated"))
    )
  def variant =
    form3.group(form.prefix("variant"), trans.site.variant(), half = true)(
      form3.select(
        _,
        translatedVariantChoicesWithVariants.map(x => x._1 -> x._2),
        disabled = disabledAfterStart
      )
    )
  def startPosition =
    form3.group(
      form.prefix("position"),
      trans.site.startPosition(),
      klass = "position",
      half = true,
      help = trans.site
        .positionInputHelp(a(href := "/editor", targetBlank)(trans.site.boardEditor.txt()))
        .some
    ):
      form3.input(_):
        tour.exists(t => !t.isCreated && t.position.isEmpty).option(disabled := true)
  def clock =
    form3.split(
      form3.group(form.prefix("clockTime"), trans.site.clockInitialTime(), half = true):
        form3.select(_, GatheringClock.timeChoices, disabled = disabledAfterStart)
      ,
      form3.group(form.prefix("clockIncrement"), trans.site.clockIncrement(), half = true):
        form3.select(_, GatheringClock.incrementChoices, disabled = disabledAfterStart)
    )
  def minutes =
    form3.group(form.prefix("minutes"), trans.site.duration(), half = true):
      form3.select(_, TournamentForm.minuteChoicesKeepingCustom(tour))
  def waitMinutes =
    form3.group(form.prefix("waitMinutes"), trans.site.timeBeforeTournamentStarts(), half = true):
      form3.select(_, TournamentForm.waitMinuteChoices)
  def description(half: Boolean) =
    form3.group(
      form.prefix("description"),
      trans.site.tournDescription(),
      help = trans.site.tournDescriptionHelp().some,
      half = half
    )(form3.textarea(_)(rows := 4))
  def entryCode =
    form3.group(
      form.prefix("password"),
      trans.site.tournamentEntryCode(),
      help = trans.site.makePrivateTournament().some,
      half = true
    )(form3.input(_)(autocomplete := "off"))
  def startDate =
    form3.group(
      form.prefix("startDate"),
      trans.arena.customStartDate(),
      help = trans.arena.customStartDateHelp().some
    )(form3.flatpickr(_))
  def advancedSettings =
    frag(
      errMsg(form.prefix("conditions")),
      p(
        strong(dataIcon := Icon.CautionTriangle, cls := "text")(trans.site.recommendNotTouching()),
        " ",
        trans.site.fewerPlayers(),
        " ",
        a(cls := "show")(trans.site.showAdvancedSettings())
      )
    )
