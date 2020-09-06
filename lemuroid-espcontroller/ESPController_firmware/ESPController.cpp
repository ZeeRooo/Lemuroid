#include <Arduino.h>
#include <ESPHelper.h>

ESPHelperClass esphelper = ESPHelperClass();

#define CLOCK_PIN 4
#define DATA_PIN 0
#define LATCH_PIN 5
/* I added two more buttons to the uC. An ugly solution for a just 8 keys NES gamepad */
//#define B_BUTTON 5
#define A_BUTTON 13

int8_t bytePosition = 0;
uint8_t input, byteArray[10], copyArray[10];
bool shouldSend = false, shouldRelease = false;

void setup()
{
    esphelper.begin();

    pinMode(DATA_PIN, INPUT);
    pinMode(CLOCK_PIN, OUTPUT);
    pinMode(LATCH_PIN, OUTPUT);

    pinMode(A_BUTTON, INPUT_PULLUP);
    /*pinMode(B_BUTTON, INPUT_PULLUP);*/

    /*
   if (bytePosition == 2 && !digitalRead(bPin))  // select + B
      esphelper.createAP();*/
    
   if (!digitalRead(A_BUTTON))
      esphelper.readPacket();

}

void readInput()
{
    delayMicroseconds(50);
    
    if (bytePosition == 0) {
        digitalWrite(LATCH_PIN, HIGH);
        delayMicroseconds(12);
        digitalWrite(LATCH_PIN, LOW);
        delayMicroseconds(6);
    }

    if (bytePosition < 8) { // My NES controller only has 8 bits, for a SNES you should count until 10 since it has 2 more buttons
        /*
           The data pin transfers an 8 bits input, so according to it's position a different button is pressed:
           0 -> A
           1 -> B
           2 -> Select
           3 -> Start
           4 -> Up
           5 -> Down
           6 -> Left
           7 -> Right
        */
        input = digitalRead(DATA_PIN);

        if (input == 0) // DATA_PIN == 0 means that a button has been pressed
            shouldSend = true;

        byteArray[bytePosition] = input;

        digitalWrite(CLOCK_PIN, HIGH);
        delayMicroseconds(6);
        digitalWrite(CLOCK_PIN, LOW);
        delayMicroseconds(6);

        bytePosition++;
    }
    else {
        bytePosition = 0;

        byteArray[8] = digitalRead(A_BUTTON); // Hardcode the position for the extra buttons
        byteArray[9] = 1; //digitalRead(B_BUTTON);

        if (byteArray[8] == 0 || byteArray[9] == 0)
          shouldSend = true;

        if (memcmp(byteArray, copyArray, sizeof(byteArray)) != 0)
            if (shouldSend) {
                esphelper.sendPacketArray(byteArray, ((uint8_t)10));

                memcpy(copyArray, byteArray, sizeof(byteArray));

                shouldRelease = true;
            }
            else if (shouldRelease) {
                copyArray[0] = 99;
                esphelper.sendPacketArray(copyArray, ((uint8_t)1));
                shouldRelease = false;
            }

        shouldSend = false;
    }
}

void loop()
{
    esphelper.handle();

    readInput();
}
