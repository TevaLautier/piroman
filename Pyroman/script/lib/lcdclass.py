# _____ _____ _____ __ __ _____ _____ 
#|     |   __|     |  |  |     |     |
#|  |  |__   |  |  |_   _|  |  |  |  |
#|_____|_____|_____| |_| |_____|_____|
#
# Use Raspberry Pi to get temperature/humidity from DHT11 sensor
# Project Tutorial Url:http://osoyoo.com/2016/12/01/use-raspberry-pi-display-temperaturehumidity-to-i2c-lcd-screen/
#  
import smbus
import time
import dht11
import RPi.GPIO as GPIO
import struct

class LCD:

  #define GPIO 14 as DHT11 data pin
  Temp_sensor=14

  # Define some device parameters
  I2C_ADDR  = 0x27 # I2C device address, if any error, change this address to 0x27
  LCD_WIDTH = 16   # Maximum characters per line

  # Define some device constants
  LCD_CHR = 1 # Mode - Sending data
  LCD_CMD = 0 # Mode - Sending command

  LCD_LINE_1 = 0x80 # LCD RAM address for the 1st line
  LCD_LINE_2 = 0xC0 # LCD RAM address for the 2nd line
  LCD_LINE_3 = 0x94 # LCD RAM address for the 3rd line
  LCD_LINE_4 = 0xD4 # LCD RAM address for the 4th line

  LCD_BACKLIGHT  = 0x08  # On
  LCD_BACKLIGHT_OFF = 0x00  # Off

  ENABLE = 0b00000100 # Enable bit

  # Timing constants
  E_PULSE = 0.0005
  E_DELAY = 0.0005
  #Open I2C interface
  #bus = smbus.SMBus(0)  # Rev 1 Pi uses 0
  bus = smbus.SMBus(1) # Rev 2 Pi uses 1

  def __init__(self):
    self.init=True

  def lcd_init(self):
    # Initialise display
    self.lcd_byte(0x33,LCD.LCD_CMD) # 110011 Initialise
    self.lcd_byte(0x32,LCD.LCD_CMD) # 110010 Initialise
    self.lcd_byte(0x06,LCD.LCD_CMD) # 000110 Cursor move direction
    self.lcd_byte(0x0C,LCD.LCD_CMD) # 001100 Display On,Cursor Off, Blink Off 
    self.lcd_byte(0x28,LCD.LCD_CMD) # 101000 Data length, number of lines, font size
    self.lcd_byte(0x01,LCD.LCD_CMD) # 000001 Clear display
    time.sleep(LCD.E_DELAY)
  
  def stop(self):
      self.lcd_byte(0x01, LCD.LCD_CMD)

  def lcd_byte(self,bits, mode):
    # Send byte to data pins
    # bits = the data
    # mode = 1 for data
    #        0 for command

    bits_high = mode | (bits & 0xF0) | LCD.LCD_BACKLIGHT
    bits_low = mode | ((bits<<4) & 0xF0) | LCD.LCD_BACKLIGHT

    # High bits
    LCD.bus.write_byte(LCD.I2C_ADDR, bits_high)
    self.lcd_toggle_enable(bits_high)

    # Low bits
    LCD.bus.write_byte(LCD.I2C_ADDR, bits_low)
    self.lcd_toggle_enable(bits_low)

  def lcd_toggle_enable(self,bits):
    # Toggle enable
    time.sleep(LCD.E_DELAY)
    LCD.bus.write_byte(LCD.I2C_ADDR, (bits | LCD.ENABLE))
    time.sleep(LCD.E_PULSE)
    LCD.bus.write_byte(LCD.I2C_ADDR,(bits & ~LCD.ENABLE))
    time.sleep(LCD.E_DELAY)

  def lcd_string(self,message,line):
    # Send string to display

    message = message.ljust(LCD.LCD_WIDTH," ")

    self.lcd_byte(line, LCD.LCD_CMD)

    for i in range(LCD.LCD_WIDTH):
      self.lcd_byte(ord(message[i]),LCD.LCD_CHR)

  def main(self):
    # Main program block
    GPIO.setwarnings(False)
    GPIO.setmode(GPIO.BCM)       # Use BCM GPIO numbers
    # Initialise display
    print("start")
    self.lcd_init()
    print("start")
    instance = dht11.DHT11(pin = LCD.Temp_sensor)
    start=100
      
    while True:
      #get DHT11 sensor value
      result = instance.read()
      print(".")
      # Send some test
      if result.is_valid():
          self.lcd_string(str(result.temperature)+"C"+"        "+str(result.humidity)+"%",LCD.LCD_LINE_1)
      self.lcd_string(str(result.temperature)+"C"+"        "+str(result.humidity)+"%",LCD.LCD_LINE_2)
      if 0:
          message=""
          message = message.ljust(LCD.LCD_WIDTH," ")
          ll=list(message)
          for i in range(LCD.LCD_WIDTH):
            ll[i]=struct.pack('>H', i+start).decode("utf8")
            print(ll[i]+str(start))
          self.lcd_string("".join(ll),LCD.LCD_LINE_2)
          start+=1
      time.sleep(3)
   
   

if __name__ == '__main__':

  lcd=LCD()
  try:
    lcd.main()
  except KeyboardInterrupt:
    pass
  finally:
    lcd.stop()
