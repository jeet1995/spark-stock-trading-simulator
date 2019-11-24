package com.trading.simulation.greedyInvestment

import java.time.LocalDate

case class InvestmentData(referentialDate: LocalDate, investmentDate: LocalDate, companySymbol: String, investedAmount: Double)
