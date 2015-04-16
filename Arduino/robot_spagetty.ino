#define FIRST 0x15
#define SECOND 0x90

char packetBuffer[512];

boolean packetReceived = false;
int packetLen = 0;
int led = 13;

#define BLINK 0

void setup() {
    pinMode(led, OUTPUT);
    Serial.begin(19200);
}

// протокол обмена таков: FIRST, SECOND, длина пакета, payload. В случае, если придётся передавать больше одного символа
// в качестве команды (например команда + аргументы) без такой хрени всё сломается.
// моя жизнь с мокроконтроллерами и уартом это сто раз показала уже.
// эта хрень ыполняется синхронно в конце loop, если в уарте что-то есть.
// Гонок нет.

void serialEvent() {
    static char protocolState, index, maxCount;
    unsigned char input = Serial.read();
    if (input == FIRST && protocolState == 0) {
        protocolState = 1;
    } else if (input == SECOND && protocolState == 1) {
        protocolState = 2;
    } else if (protocolState == 2) {
        maxCount = input;
        protocolState = 3;
        index = 0;
    } else if (protocolState == 3 && index < maxCount) {
        packetBuffer[index] = input;
        index++;
    } else {
        // если всё сломалось нахер, то обнуляем счётчики и на пакет реагировать не будем :(
        protocolState = index = maxCount = 0;
    }
    if (protocolState == 3 && index == maxCount) {
        packetReceived = true;
        packetLen = maxCount;
        protocolState = index = maxCount = 0;
    }
}

void blinkLed(int count) {
    for (int i = 0; i < count; i++) {
        digitalWrite(led, HIGH);
        delay(250);
        digitalWrite(led, LOW);
        delay(250);
    }
}

void loop() {
    if (packetReceived) {
        if (packetLen) {
            if(packetBuffer[0] == BLINK) {
                blinkLed(packetBuffer[1]);
            }
        }
        packetReceived = false;
    }
}

// Rocket Science!!