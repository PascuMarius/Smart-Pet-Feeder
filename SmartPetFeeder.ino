// ***LIBRARY DECLARATION ***
//// Scale library
#include "HX711.h"
//// Servo library
#include <Servo.h>
//// ESP WIFI client library
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <WiFiClientSecure.h>
#include <WiFiManager.h> // https://github.com/tzapu/WiFiManager
//// Real time clock library
#include <TimeLib.h>
#include <Time.h>
#include <ArduinoJson.h>
// ***START OF DECLARATION ***

////ElasticSearch info////
const String node_name = "marius.index";
const String urlPost = "/marius.index/_doc/";
const String urlGet = "/marius.index/_search/";
const String urlDelete = "/marius.index/_delete_by_query/";
const char *host = "8f9677360fc34e2eb943d737b2597c7b.us-east-1.aws.found.io";
const int httpsPort = 9243;
const String userpass = "CREDENȚIALE"; // format user:pass

////Variables for cloud////
String event = "";
int food_weight = 0;
int waterLevel = 0;
int selectedFood = 0;
int selectedWater = 0;

////Servo motor declaration////
Servo myservo;
////Scale declaration
HX711 scale;
float calibration_factor = 1899;  // 1899 this calibration factor is adjusted according to my load cell

////Water level declaration////
const int WaterSensorPin = A0;  // Analog pin
const int RelayPin = 14;

///Button declaration
const int buttonPin = 5;

///Buzzer declaration
const int piezoPin = 12;

////***SETUP***////
void setup()
{
  WiFi.mode(WIFI_STA);
  Serial.begin(115200);
  scale.begin(4, 0);
  // Connect to WiFi network
  Serial.println();
  delay(100);

  WiFiManager wifiManager;
  bool res;
  // res = wm.autoConnect();  // auto generated AP name from chipid
  // res = wm.autoConnect("AutoConnectAP"); // anonymous ap
  res = wifiManager.autoConnect("AutoConnectAP", "password"); // password protected ap
  if (!res)
  {
    Serial.println("Failed to connect");
    // ESP.restart();
  }
  else
  {
    Serial.println("connected...yeey :)");
  }

  //////Sync real time//////
  configTime(3 * 3600, 0, "pool.ntp.org", "time.nist.gov");
  Serial.println();
  Serial.print("Sync real time. This will take few seconds.");
  while (time(nullptr) <= 100000)
  {
    Serial.print("*");
    delay(100);
  }

  time_t now = time(nullptr);
  setTime(now);
  Serial.println();
  Serial.print("Date and time set!!!");
  Serial.println();
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());
  Serial.println();
  Serial.print("TimeStamp: ");
  Serial.println(getDate());

  ////SCALE SETUP////
  Serial.println("Remove all weight from scale");
  Serial.println("After readings begin, place known weight on scale for manual calibration");
  Serial.println("Press + or a to increase calibration factor");
  Serial.println("Press - or z to decrease calibration factor");
  scale.set_scale();
  scale.tare(); //Reset the scale to 0
  long zero_factor = scale.read_average();  //Get a baseline reading
  Serial.print("Zero factor: ");  //This can be used to remove the need to tare the scale. Useful in permanent scale projects.
  Serial.println(zero_factor);
  //// SERVO setup
  Serial.println(myservo.read());
  myservo.write(90);
  Serial.println(myservo.read());
  myservo.attach(13);

  /// Button and buzzer setup
  pinMode(buttonPin, INPUT_PULLUP);
  pinMode(piezoPin, OUTPUT);

  //// Set RelayPin as an output pin////
  pinMode(RelayPin, OUTPUT);
  digitalWrite(RelayPin, HIGH);
  delay(500);
}

////***LOOP***////
void loop()
{
  //// Check button status
  if (digitalRead(buttonPin) == LOW){
    event = "start";
    controlFood(20);
    controlWater();
    postDone();
  }
  //// Get, Parse, Handle response events
  String request = getRequest();
  JsonArray response = parseResponse(request);
  handleEvents(response);
  //// Control Water Level
  Serial.println("Control Water Level");
  controlWater();
  
  delay(200);
}

// ******FUNCTIONS ******

String printDigits(int digits)
{
  String dig;
  // utility function for digital clock display: prints preceding colon and leading 0
  if (digits < 10)
    dig = "0";
  dig = dig + String(digits);
  return dig;
}

////////
String getDate()
{
  String date = String(year()) + '-' + printDigits(month()) + '-' + printDigits(day()) + 'T' + printDigits(hour() + 3) + ':' + printDigits(minute()) + ':' + printDigits(second()) + 'Z';
  return date;
}

////////
void controlWater()
{
  if (selectedWater == 0)
    selectedWater = 1;
  measureWater();
  while (waterLevel <= selectedWater)
  {
    digitalWrite(RelayPin, LOW);  // open pump
    measureWater();
    delay(50);
  }
  digitalWrite(RelayPin, HIGH); // close pump
  selectedWater = 0;
}

////////
void measureWater()
{
  int value = 0;
  //Take a 50 reads average value
  for (int i = 0; i < 50; i++)
  {
    value = value + analogRead(WaterSensorPin);
  }
  int averageRead = value / 50;
  if (averageRead <= 88)
  {
    waterLevel = 0;
  }
  if (averageRead > 88 && averageRead <= 190)
  {
    waterLevel = 1;
  }
  if (averageRead > 190 && averageRead <= 240)
  {
    waterLevel = 2;
  }
  if (averageRead > 240)
  {
    waterLevel = 3;
  }
}
////////
String translateWaterLevel(int level){
  String water_level = "";
  if(level == 0){
   water_level = "Empty";
  }else if(level == 1){
    water_level = "Low";
      }else if(level == 2){
      water_level = "Medium";
        }else if(level == 3){
        water_level = "High";
          }
}
////////
void postRequest()
{
  String docPost = "{\"timestamp\":\"" + getDate() + "\"," +
                   "\"event\":\"" + "start" + "\"" + "}";
  WiFiClientSecure client;
  Serial.print("Connecting to Cloud at: ");
  Serial.println(host);
  client.setInsecure();
  int n = client.connect(host, httpsPort);
  if (!n)
  {
    Serial.println("connection failed: " + n);
    ESP.restart();
  }

  Serial.println("Document content:");
  Serial.println(docPost);
  String httpRequest = String("POST ") + urlPost + " HTTP/1.1\r\n" +
                       "Host: " + host + "\r\n" +
                       "User-Agent: esp8266pet\r\n" +
                       "Authorization: Basic " + "ZWxhc3RpYzpBV2J0bUdkYTJRN0JJMmJZcGRqeUY0cWQ=" + "\r\n" +
                       "Connection: close\r\n" +
                       "Content-Type: application/json\r\n" +
                       "Content-Length: " + docPost.length() + "\r\n\r\n" + docPost;
  Serial.print("Sending.. ");
  client.print(httpRequest);
  Serial.print(httpRequest);
  Serial.println();
  Serial.println("Done");
  String line = client.readStringUntil('\n');
  Serial.println("Reply was:");
  Serial.println(line);
  delay(100);
}

////////
void postDone()
{
  String docDone = "{\"timestamp\":\"" + getDate() + "\"," +
                   "\"event\":\"" + event + "\"," +
                   "\"food_weight[g]\":" + food_weight + "," +
                   "\"water_level\":\"" + translateWaterLevel(waterLevel) + "\"" + "}";
  WiFiClientSecure client;
  Serial.print("Connecting to Cloud at: ");
  Serial.println(host);
  client.setInsecure();
  int x = client.connect(host, httpsPort);
  if (!x)
  {
    Serial.println("connection failed: " + x);
    ESP.restart();
  }

  Serial.println("Document content:");
  Serial.println(docDone);
  String httpDone = String("POST ") + urlPost + " HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "User-Agent: esp8266pet\r\n" +
                    "Authorization: Basic " + "ZWxhc3RpYzpBV2J0bUdkYTJRN0JJMmJZcGRqeUY0cWQ=" + "\r\n" +
                    "Connection: close\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + docDone.length() + "\r\n\r\n" + docDone;
  Serial.print("Sending.. ");
  client.print(httpDone);
  Serial.print(httpDone);
  Serial.println();
  Serial.println("Done");
  String line = client.readStringUntil('\n');
  Serial.println("Reply was:");
  Serial.println(line);
  delay(100);
}

////////
String getRequest()
{
  String docGet = String("{\"query\":") + "{" + "\"match\":" + "{" + "\"event\":" + "\"start\"" + "}" + "}" + "}";
  WiFiClientSecure client;
  Serial.print("Connecting to Cloud at: ");
  Serial.println(host);
  client.setInsecure();
  int m = client.connect(host, httpsPort);
  if (!m)
  {
    Serial.println("connection failed: " + m);
    ESP.restart();
  }
  String httpGet = String("GET ") + urlGet + " HTTP/1.1\r\n" +
                   "Host: " + host + "\r\n" +
                   "User-Agent: esp8266pet\r\n" +
                   "Authorization: Basic " + "ZWxhc3RpYzpBV2J0bUdkYTJRN0JJMmJZcGRqeUY0cWQ=" + "\r\n" +
                   "Connection: close\r\n" +
                   "Content-Type: application/json\r\n" +
                   "Content-Length: " + docGet.length() + "\r\n\r\n" + docGet;

  client.print(httpGet);
  Serial.println("Done");
 while (client.available()) {
    String lineGet = client.readStringUntil('\r\n\r\n');
    if (lineGet == "\r") {
      break;
    }
  }
  String lineGet = client.readStringUntil('\n');
  Serial.println("getRequest: Reply body was:");
  Serial.println(lineGet);
  return lineGet;
}
////////
JsonArray parseResponse(String response){
  Serial.println("parseResponse: response = " + response);
  DynamicJsonDocument doc(2048);
  DeserializationError error = deserializeJson(doc, response);
  JsonArray hitsArray;
  // Test if parsing succeeds.
  if (error) {
    Serial.print(F("deserializeJson() failed: "));
    Serial.println(error.f_str());
    return hitsArray;
  }
  hitsArray = doc["hits"]["hits"];
  return hitsArray;
}
////////
void handleEvents(JsonArray events){
  Serial.printf("handleEvents events size: %d\n", events.size());
  for (JsonVariant oneEvent : events) {
       if( isCurrentTime(oneEvent) ){
          Serial.println("CurrentTime match eventTime");
          setConfiguration(oneEvent);
          event = "start";
          controlFood(selectedFood);
          controlWater();
          const char* Once = "Once";
          const char* repeat = oneEvent["_source"]["repeat"].as<const char*>();
          delay(200);
          if(strcmp (repeat, Once) == 0){
            postDelete(oneEvent["_id"].as<const char*>());
          }
          delay(200);
          postDone();
          break;
        }
    }
}
////////
#define DEBUG_LOGS 0
bool isCurrentTime(JsonVariant event) {
  time_t nowTime = now() + 3 * 60 * 60;
  time_t eventTime = getTime(event);
#if DEBUG_LOGS
  String date = String(year(nowTime)) + '-' + printDigits(month(nowTime)) + '-' + printDigits(day(nowTime)) + 'T' + printDigits(hour(nowTime)) + ':' + printDigits(minute(nowTime)) + ':' + printDigits(second(nowTime)) + 'Z';
  Serial.println("isCurrentTime now timestampString: " + date);
  
  String dateEvent = String(year(eventTime)) + '-' + printDigits(month(eventTime)) + '-' + printDigits(day(eventTime)) + 'T' + printDigits(hour(eventTime)) + ':' + printDigits(minute(eventTime)) + ':' + printDigits(second(eventTime)) + 'Z';
  Serial.println("isCurrentTime event timestampString: " + dateEvent);
#endif
  return (getHour(nowTime) == getHour(eventTime) && ( (getMinute(eventTime) >= getMinute(nowTime) - 2) && (getMinute(eventTime) <= getMinute(nowTime) ) ) );
}
////////
int getHour(time_t timest){
    struct tm *tmp = gmtime(&timest);
    int h = (timest / 3600) % 24;  
    return h;
}
////////
int getMinute(time_t timest){
    struct tm *tmp = gmtime(&timest); 
    int m = (timest / 60) % 60;
    return m;
}
////////
time_t getTime(JsonVariant event) {
  const char* timestampString = event["_source"]["timestamp"].as<const char*>();
  //Serial.printf("getTime timestampString: %s", timestampString);
  TimeElements tm;
  int yr, mnth, d, h, m, s;
  sscanf( timestampString, "%4d-%2d-%2dT%2d:%2d:%2dZ", &yr, &mnth, &d, &h, &m, &s);
  tm.Year = yr - 1970;
  tm.Month = mnth;
  tm.Day = d;
  tm.Hour = h;
  tm.Minute = m;
  tm.Second = s;

  return makeTime(tm);
}
////////
void setConfiguration(JsonVariant event) {
  int setFoodWeight = event["_source"]["setFoodWeight"].as<int>();
  selectedFood = setFoodWeight;
  const char* setWaterLevel = event["_source"]["setWaterLevel"].as<const char*>();
  if(setWaterLevel == "Empty"){
     selectedWater = 0;
    }else if(setWaterLevel == "Low"){
            selectedWater = 1;
      }else if(setWaterLevel == "Medium"){
              selectedWater = 2;
        }else if(setWaterLevel == "High"){
                selectedWater = 3;
          }
  const char* repeat = event["_source"]["repeat"].as<const char*>();   
  Serial.printf("setConfiguration repeat: %s\n", repeat);
}
////////
void postDelete(const char* id){
  String docDelete = String("{\"query\":") + "{" + "\"match\":" + "{" + "\"_id\":\"" + id + "\"" + "}" + "}" + "}";
  WiFiClientSecure client;
  Serial.print("Connecting to Cloud at: ");
  Serial.println(host);
  client.setInsecure();
  int x = client.connect(host, httpsPort);
  if (!x)
  {
    Serial.println("connection failed: " + x);
    ESP.restart();
  }
  Serial.println("Document content:");
  Serial.println(docDelete);
  String httpDelete = String("POST ") + urlDelete + " HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "User-Agent: esp8266pet\r\n" +
                    "Authorization: Basic " + "ZWxhc3RpYzpBV2J0bUdkYTJRN0JJMmJZcGRqeUY0cWQ=" + "\r\n" +
                    "Connection: close\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + docDelete.length() + "\r\n\r\n" + docDelete;
  Serial.print("Sending.. ");
  client.print(httpDelete);
  Serial.print(httpDelete);
  Serial.println();
  Serial.println("Done");
  String line = client.readStringUntil('\n');
  Serial.println("Reply was:");
  Serial.println(line);
  delay(100);
}
////////
void controlFood(int selectedWeight)
{
  playTone();
  float units;
  myservo.write(0);
  delay(100);
  while (event == "start")
  {
    scale.set_scale(calibration_factor);  //Adjust scale to calibration factor value
    // Calculate the new value in grams
    Serial.print("Reading: ");
    units = scale.get_units();
    if (units < 0)
    {
      units = 0.00;
    }
    if (units >= selectedWeight)
    {
      food_weight = int(units);
      event = "done";
      myservo.write(90);
      delay(100);
    }
    
    Serial.print(units);
    Serial.print(" grams");
    Serial.print(" calibration_factor: ");
    Serial.print(calibration_factor);Serial.println();
    /*
    // Manual calibration
    if (Serial.available())
    {
      char temp = Serial.read();
      if (temp == '+' || temp == 'a')
        calibration_factor += 1;
      else if (temp == '-' || temp == 'z')
        calibration_factor -= 1;
    }*/
  }
}

void playTone()
{
  tone(piezoPin, 500, 300);
  delay(200);
  tone(piezoPin, 600, 300);
  delay(200);
  tone(piezoPin, 500, 300);
  delay(200);
  tone(piezoPin, 600, 300);
}
