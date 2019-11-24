package com.trading.simulation.monteCarlo

import java.time.LocalDate

import com.trading.simulation.request.StockData
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{DoubleType, StringType, StructType}
import org.apache.spark.sql.{Row, SparkSession}

import scala.collection.JavaConverters._

/**
  * This singleton class runs a Monte-Carlo simulation wherein a trading date is chosen at random and a certain sum is invested.
  * Four return values corresponding to the the quartiles are computed and returned.
  **/
object MonteCarloRunner {

  /**
    * This method runs the Monte-Carlo simulation pertaining to chosing a date at random to trade.
    *
    * @param financialDataByDateRDD - The RDD containing financial information such as stock prices for a given date across companies.
    * @param totalInvestmentValue   - The total sum of value available to invest on the date chosen.
    * @param sparkSession           - The spark session associated with the spark job.
    *
    * @return Values corresponding to profits/losses across 4 quartiles (99 percentile, 75 percentile, 50 percentile, 25 percentile)
    **/
  def run(financialDataByDateRDD: RDD[(Option[LocalDate], Iterable[StockData])], totalInvestmentValue: Double, sparkSession: SparkSession) = {
    val investmentDataByRow = financialDataByDateRDD

      // Sample some entry
      .sample(true, 1)

      .map(

        financialData =>

          if (financialData._1.isDefined) {

            // Trading date
            val date = financialData._1.get

            // Length of the list of stocks available to trade
            val stockDataIterableLength = financialData._2.toArray.length


            val investmentPerCompany = totalInvestmentValue / stockDataIterableLength
            var sum = 0d

            // Sum return across all stocks invested in
            financialData._2.foreach {

              data =>


                sum += investmentPerCompany * (1 + (data.adjustedClosingPrice - data.openingPrice) / data.openingPrice)

            }

            (Some(date), sum)

          }

          else {

            (None, Double.NaN)

          }

      )

      .filter(_._1.isDefined)

      .map(x => Row.apply(x._1.get.toString, x._2))

    val schemaStruct = new StructType()
      .add("Date", StringType, false)
      .add("Change", DoubleType, false)

    val trialResultsDataFrame = sparkSession.createDataFrame(investmentDataByRow, schemaStruct)

    trialResultsDataFrame.createOrReplaceTempView("results")


    sparkSession
      .sql("select percentile(Change, array(0.99,0.75,0.5,0.25)) from results")
      .collectAsList
      .asScala
      .toString
      .replace("WrappedArray", "")
      .replace("[", "")
      .replace("]", "")
      .replace("(", "")
      .replace(")", "")
      .replace("Buffer", "")
      .split(",")
      .map(x => x.toDouble)
  }

}
