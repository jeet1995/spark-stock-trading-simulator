CS441 - Homework 2
---
Homework 3 : To gain experience with the Spark computational model in AWS cloud datacenter.
---
Name : Abhijeet Mohanty
---
### Overview

* As a part of this homework, I have created two simulations - a greedy investment model which makes investment decisions for day `t` 
based on day `t - 1` stock information and a randomized simulation using the Monte-Carlo approach to generate various scenarios. In order to 
achieve my goal, I make use of **Apache Spark** to parallelize my computations and the **Alpha Vantage API** to fetch financial data
upon which the simulation takes decisions on.   

### Setup information

* Hypervisor: VMWare Fusion Professional Version 11.0.0
* Guest OS:  CentOS
* Local development IDE: IntelliJ IDEA 2018.1
* Language: Scala v2.11.8
* Build tool: Simple build tool (SBT) v1.1.2 

### Steps to follow to set up the execution environment

* After cloning the project titled **abhijeet_mohanty_cs441_hw3** navigate to **abhijeet_mohanty_cs441_hw3/stock-trading-simulation** and run the following command to clean, compile and build the JAR
    
    `> sbt clean compile assembly`
    
* In order to fetch financial data, run the following command

    `> sbt "runMain com.trading.simulation.request.StockDataRequestExecutor <start_date> <end_date>"`
    
    * Ensure the date format is `yyyy-MM-dd`
    * The obtained financial data will be obtained in the path : **abhijeet_mohanty_cs441_hw3/stock-trading-simulation/financial-data** 

* Make sure HDP sandbox is booted up on a distribution of VMWare.

* Next, we need to copy the financial-data into the Hadoop file system as below :

    
    `> scp -P 2222 <local path to financial-data> root@sandbox-hdp.hortonworks.com:~/`
    

* Next, we need to copy the artifact JAR generated as `abhijeet_mohanty_cs441_hw2/publication-statistics-computation/target/scala-2.12/publication-statistics-computation-assembly-0.1.jar` into the Hadoop file system as below
    
    
    `> scp -P 2222 <local path to executable JAR> root@sandbox-hdp.hortonworks.com:~/`
    

* Next, we to do a secure login into the sandbox as root 
    
    
    `> ssh -p 222 root@sandbox-hdp.hortonworks.com`
    

* Next, we need to make a directory where files in `financial-data` would be placed  
    
    
    `> hdfs dfs -mkdir <input path to financial-data>`
    

* Next, we put the financial-data files in the directory we created in the preceding step 
    
    
    `> hdfs dfs -put <input path to financial-data>/`
    

* Executing the JAR
    
    `> hadoop jar <input path to executable JAR> <input path to dblp.xml>`

* Viewing the results


### Spark Job simulations

* Greedy investment

    * In this investment strategy, a trading window and an amount to be invested per day are first defined.
    * This trading window is basically the time period between present and a date in the past such that the 
      difference between these two days is the trading window.
    * The amount invested per day is an arbitrarily chosen value.
    * How this model works, albeit unsuccessfully as it results in significant losses is that an investment decision
      is made on the previous trading day's stock opening and closing price.
    * Differences are summed up across stocks and then proportionately money is invested. In case, the difference is
      negative for a stock, that particular stock is discarded.
    * `StockData` encapsulates all the stock information for a given data and company whereas `InvestmentData` encapsulates
      the investment information for a given stock on a given investment date.
    * The total returns are then computed by joining the RDD's corresponding to the stock data and the investment data for
      a given date. The contents of this RDD is then reduced to return the net return.
    * The total invested amount is nothing but daily investment value * no. of days traded on. This total invest amount is used 
      in the Monte-Carlo simulation.

* Monte-Carlo simulation

    * Here, a trading date is picked up at random and the total investment value is split up across stocks and the returns are recorded.
    * Then returns at 4 points are taken - the 99th percentile, 75th percentile, 50th percentile and the 25th percentile.
    * This computation shows that a randomized model performs better than the majority of financial models.
     

