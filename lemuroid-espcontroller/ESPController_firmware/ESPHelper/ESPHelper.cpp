#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <ArduinoOTA.h>
#include <ESP8266mDNS.h>
#include <EEPROM.h>
#include <ESPHelper.h>

WiFiServer server(9998);
WiFiClient client;

ESPHelperClass::ESPHelperClass()
{
}

void ESPHelperClass::begin()
{
    if (loadCredentials()) {
        WiFi.mode(WIFI_STA);
        WiFi.begin(ssid, &password[1]);
    }

    delay(10000);

    if (WiFi.status() != WL_CONNECTED)
        createAP();

    server.begin();
    ArduinoOTA.begin();

    pinMode(2, OUTPUT);
    digitalWrite(2, HIGH);
}

void ESPHelperClass::eepromWalker(const std::string input, const int8_t offset)
{
    short n = 0;
    bool pwd = 0;

    // Syntax: credssid%password;
    if (input[0] != ';') // checking for non-empty input
        for (short i = offset; i < input.size(); i++) {
            if (input[i] == ';') // endline
                break;

            if (!pwd)
                pwd = input[i] == '%';

            if (input[i] == '%')
                n = 0;

            if (!pwd)
                ssid[n] = input[i];
            else
                password[n] = input[i];
            n++;
        }
    else
        ssid[0] = ';';
}

void ESPHelperClass::createAP()
{
    WiFi.mode(WIFI_AP);
    WiFi.softAP("ESP_AP", "lilabonoESP"); // 192.168.4.1
}

bool ESPHelperClass::loadCredentials()
{
    EEPROM.begin(512);
    EEPROM.get(0, wifiData);

    eepromWalker(wifiData, (int8_t)0);

    EEPROM.end();

    if (ssid[0] == ';')
        return false;
    else
        return true;
}

void ESPHelperClass::saveCredentials(const std::string input)
{
    EEPROM.begin(512);

    eepromWalker(input, (int8_t)4);

    EEPROM.put(0, ssid);
    EEPROM.put(strlen(ssid), password);

    EEPROM.commit();
    EEPROM.end();
}

void ESPHelperClass::clearEeprom()
{
    EEPROM.begin(512);

    for (short i = 0; i < EEPROM.length(); i++)
        EEPROM.write(i, ';');

    EEPROM.commit();
    EEPROM.end();
}

void ESPHelperClass::sendPacketArray(const uint8_t data[], const int8_t size)
{
    if (client && client.connected())
        client.write(data, size);
    else
        client = server.available();
}

void ESPHelperClass::readPacket()
{
	digitalWrite(2, LOW);

    while (1) {
        if (server.hasClient()) {
                if (!client || !client.connected()) {
                    if (client) {
                        client.stop();
                    }
                    client = server.available(); // Client found
                    
                    client.write("something");
                    
                    client.flush();
            }
        }
        
        if (client.available()) {
            client.read(packetBuffer, 255);
            if (strncmp("cred", (char*)packetBuffer, 4) == 0) {
                saveCredentials((char*)packetBuffer);
                break;
            }
            else if (strncmp("clear", (char*)packetBuffer, 5) == 0) {
                clearEeprom();
                break;
            }
        }

        yield();
    }

    digitalWrite(2, HIGH);
}

void ESPHelperClass::handle()
{
    ArduinoOTA.handle();
}