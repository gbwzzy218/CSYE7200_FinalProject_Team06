package edu.neu.coe.CSYE7200_Team6_2018Spring.Clustering

import edu.neu.coe.CSYE7200_Team6_2018Spring.Ingest.Player.IngestPlayer
import edu.neu.coe.CSYE7200_Team6_2018Spring.Ingest.{Player}
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.sql.{DataFrame, Dataset}


object ClusteringByDistance extends Clustering {

  val inputCols = Array("player_dist_ride", "player_dist_walk")

  override def clusteringHelper(dsSeprated: Dataset[Player]): KMeansModel = {
    val k = dsSeprated.head().party_size match {
      // Numbers of clusters were generated by using elbow method called determinK in trait clustering
      case 1 => 6
      case 2 => 4
      case 4 => 5
      case _ => 0
    }

    val kmeans = new KMeans().setK(k).setSeed(1L)
    val fitDf = createDfWithFeature(dsSeprated,inputCols)
    fitDf.cache()
    kmeans.fit(fitDf)
  }

  override def dropCols(df: DataFrame): DataFrame = {
    df.drop("date", "game_size", "match_id", "match_mode", "party_size", "player_assists", "player_dbno", "player_dmg", "player_kills", "player_name", "player_survive_time", "team_id")
  }

  // the entrance of calling clustering
  def main(args: Array[String]): Unit = {
    val schema = Player.schema
    implicit val spark = Player.spark
    val dataset = IngestPlayer.ingest("sample.csv", schema)
    ClusteringByDistance.clustering(dataset)
  }
}
