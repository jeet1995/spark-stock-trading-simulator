package com.trading.simulation.utils

import org.scalatest.FunSuite

class UtilsTest extends FunSuite {


  test("getLocalDateFromLocalDateStr should return the correct date for a given string in the format (yyyy-MM-dd)") {

    val localDate = Utils.getLocalDateFromLocalDateStr("2019-10-10")

    assert(localDate.getMonth.getValue === 10)
    assert(localDate.getYear === 2019)
    assert(localDate.getDayOfMonth === 10)

  }

  test("getResourcesPath should return the resources path") {

    val resourcesPath = Utils.getResourcesPath

    assert(resourcesPath === "./stock-trading-simulation/src/main/resources/")
  }

  test("getPastDateByYearDifferential should return a date n year(s) in the past") {

    val localDate = Utils.getPastDateByYearDifferential("2019-10-10", 1)

    assert(localDate.getMonth.getValue === 10)
    assert(localDate.getYear === 2018)

  }
}
