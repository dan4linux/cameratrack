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
  if (Serial.available() > 0) {
    while (Serial.available() > 0) {
      data = Serial.read();   // read byte from port
      if (data != '\n') {     // keep reaading until \n is reached
        if (isDigit(data)) {  // ignore non-numeric data
          input += (char)data;// rebuild data byte-by-byte
        }
      } else {
        pos = input.toInt();  // get int value of string int
        input = "";           // reset buffer
        Serial.println(pos);
        myservo.write(pos);   // tell servo to go to position in variable 'pos'
        delay(15);           // don't write too fast
      }
    }
  }

} 


