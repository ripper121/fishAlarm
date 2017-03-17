/*

  Udp NTP Client

  Get the time from a Network Time Protocol (NTP) time server
  Demonstrates use of UDP sendPacket and ReceivePacket
  For more on NTP time servers and the messages needed to communicate with them,
  see http://en.wikipedia.org/wiki/Network_Time_Protocol

  created 4 Sep 2010
  by Michael Margolis
  modified 9 Apr 2012
  by Tom Igoe
  updated for the ESP8266 12 Apr 2015
  by Ivan Grokhotkov

  This code is in the public domain.

*/

#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include <Adafruit_NeoPixel.h>
// constants won't change. They're used here to
// set pin numbers:
const int buttonPin = 5;     // the number of the pushbutton pin
#define LED_PIN               13
#define NUMPIXELS             1
// variables will change:
int buttonState = 0;         // variable for reading the pushbutton status
Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, LED_PIN, NEO_GRB + NEO_KHZ800);


char ssid[] = "zwe_development";  //  your network SSID (name)
char pass[] = "zwaveeurope";       // your network password

unsigned int localPort = 2390;      // local port to listen for UDP packets

IPAddress serverIP; // time.nist.gov NTP server address
const char* serverName = "192.168.10.74";
const int port = 123;

// A UDP instance to let us send and receive packets over UDP
WiFiUDP udp;

void setup()
{
  pixels.begin();
  // initialize the pushbutton pin as an input:
  pinMode(buttonPin, INPUT_PULLUP);
  setPixel(pixels.Color(255, 0, 0));
  delay(1000);
  setPixel(pixels.Color(0, 0, 0));
  Serial.begin(115200);
  Serial.println();
  Serial.println();

  // We start by connecting to a WiFi network
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.begin(ssid, pass);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
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

void loop()
{
  // read the state of the pushbutton value:
  buttonState = digitalRead(buttonPin);

  // check if the pushbutton is pressed.
  // if it is, the buttonState is HIGH:
  if (buttonState == LOW) {
    // turn LED on:
    setPixel(pixels.Color(0, 0, 255));
    Serial.println("Bit");
    sendNTPpacket(serverIP,port);
    // wait to see if a reply is available
    delay(1000);
  } else {
    // turn LED off:
    setPixel(pixels.Color(0, 0, 0));
  }
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
