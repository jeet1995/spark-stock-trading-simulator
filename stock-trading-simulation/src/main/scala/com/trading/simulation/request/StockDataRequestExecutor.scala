package com.trading.simulation.request

import java.io.{File, FileWriter, IOException, UnsupportedEncodingException}

import com.trading.simulation.utils.{StockDataConstants, Utils}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpRequestBase}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}

import scala.collection.JavaConverters._

/**
  * This singleton class executes a HTTP GET Request to fetch stock related information
  * for a given list of companies. It then writes the response data to a .csv file.
  **/
object StockDataRequestExecutor extends LazyLogging {

  // Config settings
  private val stockRequestConfig = ConfigFactory.load("stock-request.conf")
  private val requestBodyConfig = stockRequestConfig.getConfig("request-body")
  private val requestParamsConfig = requestBodyConfig.getConfig("request-params")
  private val uriBuilder = new URIBuilder(requestBodyConfig.getString("uri"))

  // Load company details
  private val companyTickerList = requestBodyConfig.getStringList("company-stock-list").asScala.toArray[String]

  // Entry point of the program
  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      logger.error("Please enter 2 arguments - The start date and end date in (yyyy-MM-dd) format.")
      System.exit(-1)
    }
    fetchFinancialData(companyTickerList, args(0), args(1))
  }

  // Entry point to the program
  def fetchFinancialData(companyList: Array[String], startDateStr: String, endDateStr: String): Unit = {

    val httpClient = HttpClientBuilder.create.build
    val httpRequest = new HttpGet

    companyList.foreach {
      company =>
        // Build the URI with the required parameters
        uriBuilder.addParameter("datatype", requestParamsConfig.getString("datatype"))
        uriBuilder.addParameter("apikey", requestParamsConfig.getString("apikey"))
        uriBuilder.addParameter("outputsize", requestParamsConfig.getString("outputsize"))
        uriBuilder.addParameter("function", requestParamsConfig.getString("function"))
        uriBuilder.addParameter("symbol", company)

        httpRequest.setURI(uriBuilder.build)

        try {

          // Executes the request
          val response = executeRequest(httpClient, httpRequest)

          // Writes the response to a .csv file
          writeResponseToFile(response, company, startDateStr, endDateStr)

        } catch {

          case uee: UnsupportedEncodingException => logger.error("Unsupported encoding exception is thrown.")
          case cpe: ClientProtocolException => logger.error("Client protocol exception is thrown.")
          case ioe: IOException => logger.error("IO exception is thrown.")

        } finally {

          // Release connection as only two connections can be open at a given point of time
          httpRequest.releaseConnection

        }
    }
  }

  /**
    * This method executes a HTTP request.
    *
    * @param httpRequest The HTTP request to be executed.
    * @return A response object upon executing the HTTP request.
    * @throws UnsupportedEncodingException
    * @throws ClientProtocolException
    **/
  @throws[UnsupportedEncodingException]
  @throws[ClientProtocolException]
  private def executeRequest(httpClient: CloseableHttpClient, httpRequest: HttpRequestBase): CloseableHttpResponse = httpClient.execute(httpRequest)


  /**
    * This method writes a HTTP response's entity to a .csv file.
    *
    * @param response     HTTP response to be written to a file.
    * @param stockSymbol  The stock.
    * @param startDateStr The start date to consider.
    * @param endDateStr   The end date to consider.
    * @throws IOException
    **/
  @throws[IOException]
  private def writeResponseToFile(response: CloseableHttpResponse, stockSymbol: String, startDateStr: String, endDateStr: String): Unit = {

    // Initialize a file writer with a .csv extension
    val csvWriter = new FileWriter(new File("financial-data" + File.separator + stockSymbol + ".csv"))

    // Check if the response has an entity
    response.getEntity match {

      case null => logger.error("No response entity found")
      case x if x.getContentLength == 0 => logger.error("Response entity's content is empty")
      case x if x != null =>

        // Write lines to a .csv file
        scala.io.Source.fromInputStream(x.getContent).getLines().foreach {
          line =>
            processLine(line, startDateStr, endDateStr, stockSymbol) match {
              case y if y != null =>
                csvWriter.append(y)
                csvWriter.append("\n")
              case _ =>
            }

        }
    }

    // Flush
    csvWriter.flush

    // Close
    csvWriter.close
  }

  /**
    * This method processes the line to check if it has the allowed parameters.
    *
    * @param line         The line to be processed.
    * @param startDateStr The start date after which the stock timestamp should fall.
    * @param endDateStr   The end date before which the stock timestamp should fall.
    * @param stockSymbol  The symbol of the stock.
    * @return A processed line.
    **/
  private def processLine(line: String, startDateStr: String, endDateStr: String, stockSymbol: String): String = {

    val words = line.split(",")

    if (words.length == 9) {
      words(0) match {
        case "timestamp" => return line + "," + "stock" + "," + "change" // Line corresponding to the header
        case _ => // Line corresponding to the data row
          val dateOfStock = Utils.getLocalDateFromLocalDateStr(words(StockDataConstants.TIMESTAMP_INDEX))
          val startDate = Utils.getLocalDateFromLocalDateStr(startDateStr)
          val endDate = Utils.getLocalDateFromLocalDateStr(endDateStr)

          // Return a line if and only if the time stamp falls between start and end date
          if (dateOfStock.isAfter(startDate) && dateOfStock.isBefore(endDate)) {

            // Get opening price
            val openingPrice = words(StockDataConstants.OPENING_PRICE_INDEX).toFloat

            // Get adjusted closing price
            val closingPrice = words(StockDataConstants.ADJUSTED_CLOSING_PRICE_INDEX).toFloat

            return line + "," + stockSymbol + "," + (closingPrice - openingPrice).toString
          }
      }
    }
    null
  }
}