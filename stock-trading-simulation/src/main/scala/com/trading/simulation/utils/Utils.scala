package com.trading.simulation.utils

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Utils {

  def getLocalDateFromLocalDateStr(dateString: String): LocalDate = LocalDate
    .parse(dateString, DateTimeFormatter
      .ofPattern("yyyy-MM-dd"))


  def getResourcesPath: String = "." + File.separator+ "stock-trading-simulation" +
    File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator


  def getPastDateByYearDifferential(dateString: String, yearDifferential: Long): LocalDate = LocalDate
    .parse(dateString, DateTimeFormatter
      .ofPattern("yyyy-MM-dd"))
    .minusYears(yearDifferential)


}
