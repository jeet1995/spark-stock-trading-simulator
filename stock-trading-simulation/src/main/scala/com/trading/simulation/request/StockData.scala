package com.trading.simulation.request

case class StockData(stockSymbol: String, openingPrice: Double, adjustedClosingPrice: Double) {

  def getStockSymbol: String = stockSymbol

  def getOpeningPrice: Double = openingPrice

  def getAdjustedClosingPrice: Double = adjustedClosingPrice
}
