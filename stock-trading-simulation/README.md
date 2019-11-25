CS441 - Homework 3
---
Homework 3 : To gain experience with the Spark computational model in AWS cloud data center.
---
Name : Abhijeet Mohanty
---
### Overview

* As a part of this homework, I have created two simulations - a greedy investment model which makes investment decisions for day `t` 
based on day `t - 1` stock information and a randomized simulation using the Monte-Carlo approach to generate various scenarios. In order to 
achieve my goal, I make use of **Apache Spark** to parallelize my computations and the **Alpha Vantage API** to fetch financial data
upon which the simulation takes decisions on. A small demo of my deployment on AWS EMR using AWS S3 for storage [can be found on this link](https://www.youtube.com/watch?v=NT2R0RryLv0&t=481s).  

### Setup information

* Hypervisor: VMWare Fusion Professional Version 11.0.0
* Guest OS:  CentOS
* Local development IDE: IntelliJ IDEA 2018.1
* Language: Scala v2.11.8
* Build tool: Simple build tool (SBT) v1.1.2 

### Steps to follow to set up the execution environment

* After cloning the project titled **abhijeet_mohanty_cs441_hw3** navigate to **abhijeet_mohanty_cs441_hw3/stock-trading-simulation** and run the following command to clean, compile and build the JAR
    
    `> sbt clean compile assembly`
    
* In order to fetch financial data, run the following command for within the **abhijeet_mohanty_cs441_hw3/stock-trading-simulation** directory

    `> sbt "runMain com.trading.simulation.request.StockDataRequestExecutor <start_date> <end_date>"`
    
    * Ensure the date format is `yyyy-MM-dd`
    * The obtained financial data will be obtained in the path : **abhijeet_mohanty_cs441_hw3/stock-trading-simulation/financial-data** 

* Make sure HDP sandbox is booted up on a distribution of VMWare.

* Next, we need to copy all files in financial-data into the Hadoop file system as below :

    
    `> scp -P 2222 <local path to financial-data>/*.csv root@sandbox-hdp.hortonworks.com:~/`
    

* Next, we need to copy the artifact JAR generated as `abhijeet_mohanty_cs441_hw3/stock-trading-simulation/target/scala-2.11/stock-trading-simulation-assembly-0.1.jar` into the Hadoop file system as below
    
    
    `> scp -P 2222 <local path to executable JAR> root@sandbox-hdp.hortonworks.com:~/`
    

* Next, we to do a secure login into the sandbox as root 
    
    
    `> ssh -p 222 root@sandbox-hdp.hortonworks.com`
    

* Next, we need to make a directory where files in `financial-data` would be placed  
    
    
    `> hdfs dfs -mkdir <input path to financial-data>`
    

* Next, we put the financial-data files (.csv) in the directory we created in the preceding step 
    
    
    `> hdfs dfs -put <input path to financial-data>/`
    
* Executing the spark job
    
   * Execution in cluster mode
    
    `> spark-submit --deploy-mode cluster stock-trading-simulation-assembly-0.1.jar <path to input files> <path to output> localMode=false`
 
   * Execution in local mode
    
    `> spark-submit stock-trading-simulation-assembly-0.1.jar <path to input files> <path to output> localMode=true`
     
    
   * Ensure to specify a trailing `/` when specifying the input or the output path. 

* Viewing the results
    * The results will be output in the path `<path to output>/results/`

### Spark Job simulations

* Greedy investment

    * In this investment strategy, a trading window and an amount to be invested per day are first defined.
    * This trading window is basically the time period between present and a date in the past such that the 
      no. of trading days these two days is the trading window.
    * The amount invested per day is an arbitrarily chosen value.
    * How this model works, albeit unsuccessfully as it results in  losses is that an investment decision
      is made on the previous trading day's stock opening and closing price.
    * Differences are summed up across stocks and then proportionately money is invested. In case, the difference is
      negative for a stock, that particular stock is discarded.
    * `StockData` encapsulates all the stock information for a given data and company whereas `InvestmentData` encapsulates
      the investment information for a given stock on a given investment date.
    * The total returns are then computed by joining the RDD's `financialDataByDateRDD` & `investmentDataByDateRDD` corresponding to the stock data and the investment data for
      a given date. The contents of this RDD is then reduced to return the net return.
    * The total invested amount is nothing but `daily investment value * no. of days` traded on. This total invested amount is used 
      in the Monte-Carlo simulation.

* Monte-Carlo simulation

    * Here, a trading date is picked up at random and the total investment value is split up across stocks and the returns are recorded.
    * The trading date and the return for that date is recorded and stored in a data frame.
    * Then returns at 4 points are taken - the 99th percentile, 75th percentile, 50th percentile and the 25th percentile.

### Alpha Vantage API
* In order to obtain historical and real time data on stocks, I make use of the [Alpha Vantage API](https://www.alphavantage.co/documentation/) for a given list of
S&P symbols such as `GOOGL, INTC, KO, SAP`. 
* The `StockDataRequestExecutor` class is responsible for hitting this API and storing the response in a .csv file, depending
on the dates passed as command-line arguments as mentioned above.  
* The request body which I build is as follows : 

````
request-body = {

  uri = "https://www.alphavantage.co/query"
  company-stock-list = [SAP, INTC, KO, GOOGL]

  request-params = {
    datatype = csv
    apikey = 1D4RH4TSOM8E7O4F
    outputsize = full
    function = TIME_SERIES_DAILY_ADJUSTED
  }
}
````


### Results

* Here is the output which depicts a comparison between a greedy model and a Monte-Carlo randomized model with the same investment values 
   
````
Trading period : 2014-08-01 to 2018-07-20
Total investment value : 18130.0
Total return of greedy investment : 18124.914981409474
Monte Carlo simulation randomization with the same investment value as greedy simulation
99 percentile return : 17926.129309524214
75 percentile return : 17393.696705211085
50 percentile return : 17080.679641882387
25 percentile return : 16778.435322771842
````
   
   
````
Trading period : 2016-12-16 to 2018-07-20
Total investment value : 2170.0
Total return of greedy investment : 2170.2023516104964
Monte Carlo simulation randomization with the same investment value as greedy simulation
99 percentile return : 2157.255482529495
75 percentile return : 2102.9590197613393
50 percentile return : 2086.6468425756034
25 percentile return : 2071.7119333557675
````

### Future improvements

* The greedy model could be combined with a randomized model of selecting stocks and distributed across several multiverses
wherein one evaluates how gains plateau or how losses can be curbed.
* Stock information instead of being loaded into .csv files could be loaded into dataframes which would mimic a sort of streaming
of financial data.
* More configurability when it comes to building a portfolio - like choosing a list of S&P companies to invest in,  
using a pre-existing fund rather than a daily fund limit etc.     
* An issue which I could not fix is running multiple configurations and writing their results in cluster mode although it works in local mode. It kept on stating that the output directory 
is already created even though I have separate folder structures for my simulations through their `simulation-id`'s.     
* The results in the video and the README do not match due to a glitch when running the application while making the video. It has been rectified in the repository but not in the video.

