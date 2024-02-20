import javax.net.ssl.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

def executeGetRequest(def endpoint) {
    def url = "${p:URL}"
    def username = "${p:Username}"
    def password = "${p:Password}"
    String encoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8"))

    // Request URL
    def uri = url + "/${endpoint}"

    // Creating object to accept all hostnames
    def nullHostnameVerifier = [
            verify: { hostname, session -> true }
    ]

    // Accept all certificates, ignore SSL check
    def sc = SSLContext.getInstance("SSL")
    def trustAll = [
            getAcceptedIssuers: {},
            checkClientTrusted: { a, b -> },
            checkServerTrusted: { a, b -> }
    ]
    sc.init(null, [trustAll as X509TrustManager] as TrustManager[], null)
    HttpsURLConnection.defaultSSLSocketFactory = sc.socketFactory
    HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)

    // Setting up connection
    def connection = new URL(uri).openConnection() as HttpURLConnection

    // Setting Headers
    connection.setRequestMethod("GET")
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("Authorization", "Basic " + encoding)

    // Get the response
    def responseCode = connection.responseCode
    println "Response Code: ${responseCode}"

    if (responseCode == 200) {
        // Get the response body
        def response = connection.inputStream.withCloseable { inStream ->
            inStream.text
        }
        println "Response Body:"
        def parsedJson = new groovy.json.JsonSlurper().parseText(response)
        def prettyJson = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(parsedJson))
        println prettyJson
    } else {
        // Get the error response body
        def errorResponse = connection.errorStream?.withCloseable { errorStream ->
            errorStream.text
        }
        println "Error Response Body:"
        println errorResponse
    }
}

def ExecuteMethod(def method, def endpoint, def requestBody = null) {
    def url = "https://localhost:9200"
    def username = "elastic"
    def password = "owIVxv2Uf7FJxHKqiivJ"
    String encoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8"))

    // Request URL
    def uri = url + "/${endpoint}"

    // Creating object to accept all hostnames
    def nullHostnameVerifier = [
            verify: { hostname, session -> true }
    ]

    // Accept all certificates, ignore SSL check
    def sc = SSLContext.getInstance("SSL")
    def trustAll = [
            getAcceptedIssuers: {},
            checkClientTrusted: { a, b -> },
            checkServerTrusted: { a, b -> }
    ]
    sc.init(null, [trustAll as X509TrustManager] as TrustManager[], null)
    HttpsURLConnection.defaultSSLSocketFactory = sc.socketFactory
    HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)

    // Setting up connection
    def connection = new URL(uri).openConnection() as HttpURLConnection

    // Setting Headers
    connection.setRequestMethod(method)
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("Authorization", "Basic " + encoding)

    // Setting Content-Type header
    if (["PUT", "POST"].contains(method)) {
        connection.setRequestProperty("Content-Type", "application/json")
    }

    // Handling request body for PUT and POST
    if (["PUT", "POST"].contains(method) && requestBody != null) {
        connection.setDoOutput(true)
        connection.getOutputStream().withCloseable { os ->
            os.write(requestBody.getBytes("UTF-8"))
        }
    }

    // Get the response
    def responseCode = connection.responseCode
    println "Response Code: ${responseCode}"

    if (responseCode == 200 || responseCode == 201) {
        // Get the response body
        def response = connection.inputStream.withCloseable { inStream ->
            inStream.text
        }
        println "Response Body:"
        def parsedJson = new groovy.json.JsonSlurper().parseText(response)
        def prettyJson = groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(parsedJson))
        println prettyJson
    } else {
        // Get the error response body
        def errorResponse = connection.errorStream?.withCloseable { errorStream ->
            errorStream.text
        }
        println "Error Response Body:"
        println errorResponse
    }
}

// Path to your input file
def filePath = '${p:JSONFileName}'

// List to store extracted information
def extractedData = []

// Read the file content
def fileContent = new File(filePath).text

// Split the content by lines
def lines = fileContent.readLines()

// Counter for labeling queries
def queryCounter = 1

// Map to store current query information
def currentQuery = [:]

// StringBuilder to accumulate data in a single line
def dataLine = new StringBuilder()

// Iterate over lines and extract request method and suburi
lines.each { line ->
    def trimmedLine = line.trim()
    if (trimmedLine.startsWith('GET') || trimmedLine.startsWith('POST') || trimmedLine.startsWith('PUT') || trimmedLine.startsWith('DELETE')) {
        // If data is present, add the data to the current query map
        currentQuery['data'] = dataLine.toString().replaceAll(/"""(.*)"""/, '$1')
        extractedData << currentQuery

        // Reset dataLine and currentQuery for the next query
        dataLine = new StringBuilder()
        currentQuery = [:]

        def parts = trimmedLine.split(' ', 2)
        def method = parts[0]
        def suburi = parts[1]

        currentQuery['queryNumber'] = queryCounter
        currentQuery['method'] = method
        currentQuery['suburi'] = suburi

        //println "query${queryCounter} :Method : ${method}\nsuburi : ${suburi}"
        queryCounter++
    } else {
        // Accumulate data into a single line
        dataLine.append(line.trim())
    }
}

// Add the last query to the extracted data list
currentQuery['data'] = dataLine.toString().replaceAll(/"""(.*)"""/, '$1')
extractedData << currentQuery

// Iterate over the extracted data
extractedData.each { query ->
    if (!query['queryNumber'].toString().toLowerCase().equals("null")) {
        println "Query Number: ${query['queryNumber']}"
        println "Method: ${query['method']}"
        println "Suburi: ${query['suburi']}"
        if (!query['data'].toString().isEmpty()) {
            println "Data: ${query['data']}"
        }

        if (query['method'].toString().toLowerCase().equals("get")){
            executeGetRequest(query['suburi'].toString())
        } else {
            if (!query['data'].toString().isEmpty()) {
                ExecuteMethod(query['method'].toString(),query['suburi'],query['data'].toString())
            } else {
                ExecuteMethod(query['method'].toString(),query['suburi'],'')
            }
        }
    }
    println "--------------------------"
    sleep(5000)
}
