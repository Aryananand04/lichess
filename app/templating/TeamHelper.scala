package lila.app
package templating

import controllers.routes
import controllers.team.routes.Team as teamRoutes
import scalatags.Text.all.Tag

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.web.ui.AssetHelper
import lila.core.team.LightTeam
import lila.team.Team

trait TeamHelper:
  self: RouterHelper =>

  def assetHelper: AssetHelper
  def env: Env

  def isMyTeamSync(teamId: TeamId)(using ctx: Context): Boolean =
    ctx.userId.exists { env.team.api.syncBelongsTo(teamId, _) }

  def teamIdToLight(id: TeamId): LightTeam =
    env.team.lightTeamSync(id).getOrElse(LightTeam(id, id.value, none))

  def teamLink(id: TeamId, withIcon: Boolean = true): Tag =
    teamLink(teamIdToLight(id), withIcon)

  def teamLink(team: LightTeam, withIcon: Boolean): Tag =
    a(
      href     := teamRoutes.show(team.id),
      dataIcon := withIcon.option(lila.ui.Icon.Group),
      cls      := withIcon.option("text")
    )(team.name, teamFlair(team))

  def teamLink(team: Team, withIcon: Boolean): Tag = teamLink(team.light, withIcon)

  def teamFlair(team: Team): Option[Tag]      = team.flair.map(teamFlair)
  def teamFlair(team: LightTeam): Option[Tag] = team.flair.map(teamFlair)

  def teamFlair(flair: Flair): Tag =
    img(cls := "uflair", src := assetHelper.flairSrc(flair))

  def teamForumUrl(id: TeamId) = routes.ForumCateg.show("team-" + id)

  lazy val variantTeamLinks: Map[chess.variant.Variant.LilaKey, (LightTeam, Frag)] =
    lila.team.Team.variants.view
      .mapValues: team =>
        (team, teamLink(team, true))
      .toMap
