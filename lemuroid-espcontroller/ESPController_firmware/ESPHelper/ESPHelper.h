#ifndef ESPHelper_h
#define ESPHelper_h

#include <string>

class ESPHelperClass
{
public:
    ESPHelperClass();
    void begin();
    void handle();
    bool loadCredentials();
    void saveCredentials(const std::string input);
    void eepromWalker(const std::string input, const int8_t offset);
    void clearEeprom();
    void createAP();
    void sendPacketArray(const uint8_t data[], const int8_t size);
    void readPacket();
private:
    char ssid[100];
    char password[100];
    char wifiData[100];
    uint8_t packetBuffer[255];
};

#endif