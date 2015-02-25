#include <SoftwareSerial.h>
//#include <Console.h>
#include <Servo.h> 

Servo myservo;  // create servo object to control a servo 
// a maximum of eight servo objects can be created 

int pos = 0;    // variable to store the servo position 
int x = 0;
String input = "";
int data;



void setup() 
{ 
  myservo.attach(9);  // attaches the servo on pin 9 to the servo object 
  pinMode(13, OUTPUT);
  Serial.begin(9600);
} 


void loop() 
{ 
  //  while (!Console);
  //Serial.println("Send position followed by '\n'");
  if (Serial.available() > 0) {
    while (Serial.available() > 0) {
      data = Serial.read();
      if (data != '\n') {
        if (isDigit(data)) {
          input += (char)data;
        }
      } else {
        digitalWrite(13, HIGH);
        delay(150);
        digitalWrite(13, LOW);
        // read the oldest byte in the serial buffer:
        pos = input.toInt();
        input = "";
        Serial.println(pos);
        // if it's a capital H (ASCII 72), turn on the LED:
        myservo.write(pos);              // tell servo to go to position in variable 'pos' 
        delay(1000);                       // waits 15ms for the servo to reach the position       
      }
    }
  }

} 


