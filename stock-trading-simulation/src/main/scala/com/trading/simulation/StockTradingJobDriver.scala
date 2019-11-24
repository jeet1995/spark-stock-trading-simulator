package com.trading.simulation

import java.io.File

import com.trading.simulation.greedyInvestment.GreedyInvestment
import com.trading.simulation.monteCarlo.MonteCarloRunner
import com.trading.simulation.utils.SimulationResult
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConverters._

object StockTradingJobDriver extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("Starting stock trading job driver")

    // Load application config
    val applicationConfig = ConfigFactory.load().getConfig("application-conf")
    val simulationConfigs = applicationConfig.getConfigList("simulation-configurations").asScala.toList

    // Run the spark job
    simulationConfigs.foreach {
      simulationConfig =>
        runSimulation(applicationConfig, simulationConfig)
    }
  }

  private def runSimulation(applicationConfig: Config, simulationConfig: Config): Unit = {

    val sparkConf = new SparkConf().setAppName(applicationConfig.getString("spark-application-name"))

    sparkConf.setMaster("local[*]")

    val sparkSession = SparkSession.builder.master("local[*]").getOrCreate

    val sparkContext = SparkContext.getOrCreate(sparkConf)

    val financialDataRDD: RDD[String] = sparkContext.textFile("file:///Users/SubrataMohanty/Documents/abhijeet_mohanty_cs441_hw3/stock-trading-simulation/financial-data/*.csv")

    val simulationResult = GreedyInvestment.run(financialDataRDD, sparkContext, simulationConfig)

    val randomizedInvestmentResults = MonteCarloRunner.run(simulationResult.financialDataRDD, simulationResult.totalInvestmentValue, sparkSession)

    writeResult(simulationResult, randomizedInvestmentResults, sparkContext, simulationConfig)

    sparkContext.stop
  }

  private def writeResult(result: SimulationResult, randomizedInvestmentResults: Array[Double], sparkContext: SparkContext, simulationConfig: Config): Unit = {

    val tradingPeriodRDD = sparkContext.parallelize(Seq("Trading period : " + result.sortedDates(result.sortedDates.length - 1) + " to " + result.sortedDates(0)))
    val totalInvestmentRDD = sparkContext.parallelize(Seq("Total investment value : " + result.totalInvestmentValue))
    val totalReturnRDD = sparkContext.parallelize(Seq("Total return of greedy investment : " + result.returns))

    val monteCarloHeaderRDD = sparkContext.parallelize(Seq("Monte Carlo simulation randomization with the same investment value as greedy simulation"))
    val firstQuartileReturnsRDD = sparkContext.parallelize(Seq("99 percentile return : " + randomizedInvestmentResults(0)))
    val secondQuartileReturnsRDD = sparkContext.parallelize(Seq("75 percentile return : " + randomizedInvestmentResults(1)))
    val thirdQuartileReturnsRDD = sparkContext.parallelize(Seq("50 percentile return : " + randomizedInvestmentResults(2)))
    val fourthQuartileReturnsRDD = sparkContext.parallelize(Seq("25 percentile return : " + randomizedInvestmentResults(3)))


    tradingPeriodRDD
      .union(totalInvestmentRDD)
      .union(totalReturnRDD)
      .union(monteCarloHeaderRDD)
      .union(firstQuartileReturnsRDD)
      .union(secondQuartileReturnsRDD)
      .union(thirdQuartileReturnsRDD)
      .union(fourthQuartileReturnsRDD)
      .coalesce(1, true)
      .saveAsTextFile("file:///Users/SubrataMohanty/Documents/abhijeet_mohanty_cs441_hw3/results" + File.separator + "result" + simulationConfig.getString("simulation-id"))
  }

}



