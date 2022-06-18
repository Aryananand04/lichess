package lila.tutor

import chess.Color

import lila.common.LilaOpeningFamily

case class TutorOpenings(
    colors: Color.Map[TutorColorOpenings]
)

case class TutorColorOpenings(
    families: List[TutorOpeningFamily]
)

case class TutorOpeningFamily(
    family: LilaOpeningFamily,
    games: TutorMetric[TutorRatio],
    performance: TutorMetric[Double],
    acpl: TutorMetricOption[Double]
)