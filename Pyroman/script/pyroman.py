import smbus
import time
import lcd
import sys
from lib import import dht11
import RPi.GPIO as GPIO
import struct
from lib import stepper
from lib import BMP180
from lib import meteo

class Robot:
  RPM=16

  def __init__(self):
    GPIO.setmode(GPIO.BCM)
    self.lcd=lcd
    lcd.lcd_init();
    self.gauche = Motor([6,13,19,26])
    self.gauche.rpm = Robot.RPM
    self.gauche.mode = 3

    self.droite = Motor([12,16,20,21])
    self.droite.rpm = Robot.RPM
    self.droite.sens = -1
    self.droite.mode = 3

    self.tete = Motor([4,17,27,22])
    self.tete.rpm = Robot.RPM
    self.tete.mode = 3

    self.meteo=Meteo()

  def start(self):
    print "Start PiRoMan"
    self.lcd.start()

  def stopAll(self):
    self.stop()
    self.lcd.stopBackLight()

  def stop(self):
    print "Stop PiRoMan"


    self.lcd.stop()
    # retrun to original position
    self.droite.setAngle(0)
    self.gauche.setAngle(0)
    self.tete.setAngle(0)

    self.gauche.stop()
    self.droite.stop()
    self.tete.stop()
  def main(self):
    print("Start PiRoMan sample")
    #self.lcd.main()

    self.temp = self.meteo.read_temperature()
    self.temp2 = self.meteo.read_temperature2()
    self.pressure = self.meteo.read_pressure()
    self.altitude = self.meteo.read_altitude()
    self.humidity = 0.0
    #self.meteo.read_humidity()
    self.droite.setAngle(90)
    self.gauche.setAngle(120)
    for i in range(10):
      self.tete.setAngle(10)
      self.tete.setAngle(-10)
    
    for i in range(10):
        self.droite.setAngle(90)
        self.droite.setAngle(80)
        self.gauche.setAngle(110)
        self.gauche.setAngle(120)
    self.droite.setAngle(90)
    self.gauche.setAngle(120)
    for i in range(10):
        self.droite.setAngle(110)
        self.droite.setAngle(120)
        self.gauche.setAngle(80)
        self.gauche.setAngle(90)
    self.droite.setAngle(30)
    self.gauche.setAngle(180)
    for i in range(10):
        self.droite.setAngle(40)
        self.droite.setAngle(30)
        self.gauche.setAngle(160)
        self.gauche.setAngle(180)
    
    self.gauche.setAngle(80)
    
    for i in range(2):
        sleep(1)
        self.lcd.line(0,"   |       |   ")
        self.lcd.line(1,"   |=======|   ")
        sleep(1)
        self.lcd.line(0,"   |-------| ")
        self.lcd.line(1,"   |___v___| ")
        sleep(1)
    
    self.lcd.line(1,"%.2f\x2DC" % self.temp+ "  %.2f\x2DC" % self.temp2)
    self.lcd.line(2,"%.2f hPa" % (self.pressure / 100.0))
    
    
    



if __name__ == '__main__':

  robot=Robot()
  try:
    #print("arg="+str(len(sys.argv))+":"+sys.argv[1]+" "+sys.argv[0]+" :"+str(sys.argv[1]=="stop"))
    if len(sys.argv)==2:
      if sys.argv[1]=="start":
        robot.start()
      if sys.argv[1]=="stop":
        print "dddd"
        robot.stopAll()
    if len(sys.argv)==1:
      robot.main()
  except KeyboardInterrupt:
    pass
  finally:
    robot.stop()
