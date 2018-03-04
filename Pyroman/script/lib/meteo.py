import dht11
from BMP180 import BMP180

Temp_sensor=14

class Meteo:
  def __init__(self):
    self.bmp = BMP180()
    self.temphum = dht11.DHT11(pin = Temp_sensor)
  
  def read_temperature(self):
    return self.bmp.read_temperature()
  def read_temperature2(self):
    while True:
      result = self.temphum.read()
      if result.is_valid():
        return result.temperature
  def read_pressure(self):
	return self.bmp.read_pressure()
  def read_altitude(self):
	return self.bmp.read_altitude()
  def read_humidity(self):
    while True:
      result = self.temphum.read()
      if result.is_valid():
        return result.humidity

   
