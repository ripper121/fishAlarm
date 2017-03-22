#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include <Adafruit_NeoPixel.h>
// Required for LIGHT_SLEEP_T delay mode
extern "C" {
#include "user_interface.h"
}
// constants won't change. They're used here to
// set pin numbers:
#define SOCKETPLUG            5
#define BUTTON                12
#define LED_PIN               13
#define NUMPIXELS             1
// variables will change:
int buttonState = 0;         // variable for reading the pushbutton status
Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, LED_PIN, NEO_GRB + NEO_KHZ800);


char ssid[] = "AndroidAP";  //  your network SSID (name)
char pass[] = "12345678";       // your network password

unsigned int localPort = 2390;      // local port to listen for UDP packets

IPAddress serverIP; // time.nist.gov NTP server address
const char* serverName = "192.168.43.1";
const int port = 8080;

// A UDP instance to let us send and receive packets over UDP
WiFiUDP udp;

void setup()
{
  pixels.begin();
  // initialize the pushbutton pin as an input:
  pinMode(SOCKETPLUG, INPUT_PULLUP);
  pinMode(BUTTON, INPUT_PULLUP);
  setPixel(pixels.Color(255, 0, 0));
  delay(1000);
  setPixel(pixels.Color(0, 0, 0));
  Serial.begin(115200);
  Serial.println();
  Serial.println();
}

void loop()
{
  if (digitalRead(SOCKETPLUG) == LOW || digitalRead(BUTTON) == LOW ) {
    // turn LED on:
    connectWifi();
    setPixel(pixels.Color(0, 0, 255));
    Serial.println("Alarm");
    sendNTPpacket(serverIP, port);
    // wait to see if a reply is available
    delay(1000);
  } else {
    // turn LED off:
    setPixel(pixels.Color(0, 0, 0));
    WiFi.disconnect();
    WiFi.forceSleepBegin();
    delay(10);
  }
}

void connectWifi()
{
  WiFi.forceSleepWake();
  delay(1);
  // We start by connecting to a WiFi network
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, pass);
  bool toggleLed = true;
  while (WiFi.status() != WL_CONNECTED) {
    delay(200);
    Serial.print(".");
    if (toggleLed) {
      setPixel(pixels.Color(0, 0, 255));
    } else {
      setPixel(pixels.Color(0, 0, 0));
    }
    toggleLed = !toggleLed;
  }
  Serial.println("");

  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  Serial.println("Starting UDP");
  udp.begin(localPort);
  Serial.print("Local port: ");
  Serial.println(udp.localPort());
  //get a random server from the pool
  WiFi.hostByName(serverName, serverIP);
}

void setPixel(uint32_t pixelColor) {
  pixels.setPixelColor(0, pixelColor);
  pixels.show();
}

// send an NTP request to the time server at the given address
unsigned long sendNTPpacket(IPAddress& address, int port)
{
  Serial.println("sending NTP packet...");
  // all NTP fields have been given values, now
  // you can send a packet requesting a timestamp:
  udp.beginPacket(address, port); //NTP requests are to port 123
  udp.write("Alarm", 5);
  udp.endPacket();
}
