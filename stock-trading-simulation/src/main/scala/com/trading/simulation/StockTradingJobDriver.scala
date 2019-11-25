package com.trading.simulation

import com.trading.simulation.greedyInvestment.GreedyInvestment
import com.trading.simulation.monteCarlo.MonteCarloRunner
import com.trading.simulation.utils.SimulationResult
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConverters._

/**
  * This singleton class represents the entry point for the Spark application.
  * */
object StockTradingJobDriver extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("Starting stock trading job driver")

    if (args.length < 2) {
      logger.error("Two arguments corresponding to the input and output paths are to be specified.")
      System.exit(-1)
    }

    // Load application config
    val applicationConfig = ConfigFactory.load().getConfig("application-conf")
    val simulationConfigs = applicationConfig.getConfigList("simulation-configurations").asScala.toList

    // Run the spark job
    simulationConfigs.foreach {
      simulationConfig =>

        val localMode = !(args.length == 3 && args(2).split("=")(1).toBoolean == false)

        runSimulation(applicationConfig, simulationConfig, args(0), args(1), localMode)

    }
  }

  private def runSimulation(applicationConfig: Config, simulationConfig: Config, inputPath: String, outputPath: String, localMode: Boolean): Unit = {

    val sparkConf = new SparkConf().setAppName(applicationConfig.getString("spark-application-name"))

    if (localMode) {
      sparkConf.setMaster("local[*]")
    }
    val sparkSession = SparkSession.builder.getOrCreate

    val sparkContext = SparkContext.getOrCreate(sparkConf)

    // Load .csv files
    val financialDataRDD: RDD[String] = sparkContext.textFile(inputPath + "*.csv")

    // Greedy investment results
    val simulationResult = GreedyInvestment.run(financialDataRDD, sparkContext, simulationConfig)

    // Monte-Carlo results
    val randomizedInvestmentResults = MonteCarloRunner.run(simulationResult.financialDataRDD, simulationResult.totalInvestmentValue, sparkSession)

    writeResult(simulationResult, randomizedInvestmentResults, sparkContext, simulationConfig, outputPath)

    sparkContext.stop
  }

  private def writeResult(result: SimulationResult, randomizedInvestmentResults: Array[Double], sparkContext: SparkContext, simulationConfig: Config, outputPath: String): Unit = {

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
      .repartition(1)
      .saveAsTextFile(outputPath + "result" + "-" + simulationConfig.getString("simulation-id"))
  }

}



