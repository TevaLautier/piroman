import smbus
import sys
import RPi.GPIO as GPIO
import struct

from time import *
from lib import lcd
from lib import dht11
from lib import stepper
from lib import BMP180
from lib import meteo

class Robot:
  RPM=16

  def __init__(self):
    GPIO.setmode(GPIO.BCM)
    self.lcd=lcd
    lcd.lcd_init();
    self.gauche = stepper.Motor([6,13,19,26])
    self.gauche.rpm = Robot.RPM
    self.gauche.sens = -1
    self.gauche.mode = 3

    self.droite = stepper.Motor([12,16,20,21])
    self.droite.rpm = Robot.RPM
    self.droite.sens = 1
    self.droite.mode = 3

    self.tete = stepper.Motor([4,17,27,22])
    self.tete.rpm = Robot.RPM
    self.tete.mode = 3

    self.meteo=meteo.Meteo()

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
      print("Use python pyroman.py start|stop|test")
  
  def test(self):
    print("Start PiRoMan sample")
    
    self.temp = self.meteo.read_temperature()
    self.temp2 = self.meteo.read_temperature2()
    self.pressure = self.meteo.read_pressure()
    self.altitude = self.meteo.read_altitude()
    self.humidity = self.meteo.read_humidity()
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
    
    
    sleep(10)
    self.lcd.stopBackLight()



if __name__ == '__main__':
  GPIO.setwarnings(False)  
  robot=Robot()
  try:
    #print("arg="+str(len(sys.argv))+":"+sys.argv[1]+" "+sys.argv[0]+" :"+str(sys.argv[1]=="stop"))
    if len(sys.argv)==2:
      if sys.argv[1]=="start":
        robot.start()
      if sys.argv[1]=="stop":
        robot.stopAll()
      if sys.argv[1]=="test":
        robot.test()
    if len(sys.argv)==1:
      robot.main()
  except KeyboardInterrupt:
    pass
  finally:
    robot.stop()
