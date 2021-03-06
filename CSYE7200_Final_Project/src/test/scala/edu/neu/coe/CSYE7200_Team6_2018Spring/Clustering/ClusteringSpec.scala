package edu.neu.coe.CSYE7200_Team6_2018Spring.Clustering

import edu.neu.coe.CSYE7200_Team6_2018Spring.Ingest.Player
import edu.neu.coe.CSYE7200_Team6_2018Spring.Ingest.Player.IngestPlayer
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.linalg.{DenseVector, SparseVector}
import org.apache.spark.sql.{DataFrame, Dataset}
import org.scalatest.{FlatSpec, Matchers}


class ClusteringSpec extends FlatSpec with Matchers {


  val schema = Player.schema
  implicit val spark = Player.spark
  val dataset = IngestPlayer.ingest("sample.csv", schema)
  val filteredDS = IngestPlayer.filterPlayers(dataset)

  class TestClustering extends Clustering {
    val inputCols = Array("player_dist_ride", "player_dist_walk", "player_dmg", "player_kills")
    override def clusteringHelper(dsSeprated: Dataset[Player]): KMeansModel = {
      val k = dsSeprated.head().party_size match {
        // Numbers of clusters were generated by using elbow method called determinK in trait clustering
        case 1 => 3
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
      df.drop("date", "game_size", "match_id", "match_mode", "player_assists", "player_dbno", "player_name", "player_survive_time", "team_id")
    }
  }
  object TestClustering extends TestClustering

  behavior of "Clustering"

  it should "work for creating the input column of clustering" in {
    val scaledDf = TestClustering.createDfWithFeature(filteredDS, TestClustering.inputCols)
    //check column number
    scaledDf.columns.length shouldBe 8
    //check column names
    val colNames = for(dfField <-  scaledDf.schema) yield dfField.name
    colNames should matchPattern {
      case List("party_size","player_dist_ride","player_dist_walk","player_dmg","player_kills","team_placement","unscaled_features","features") =>
    }
    //check "features" column type
    val rowtypes = scaledDf.select("features").collect().map(_(0))
    val rows = for(row <- rowtypes;if row.isInstanceOf[DenseVector] || row.isInstanceOf[SparseVector]) yield row
    rows should have length 9779
  }

  it should "work for determining K value" in {
    val scaledDf = TestClustering.createDfWithFeature(filteredDS, TestClustering.inputCols)
    val kPairs = TestClustering.determinK(scaledDf)
    kPairs should have length 8
    kPairs shouldBe an [IndexedSeq[_]]
    for(pair <- kPairs) yield pair shouldBe an [(Int,Double)]
  }

  it should "work for creating KMeansModel" in{
    val models = TestClustering.clustering(dataset)
    models should have length 3
    models.foreach(_ shouldBe a [KMeansModel])
    models(0).clusterCenters should have length 3
    models(1).clusterCenters should have length 4
    models(2).clusterCenters should have length 5
  }
}

