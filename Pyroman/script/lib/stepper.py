#!/usr/bin/env python

# This code is written by Stephen C Phillips.
# It is in the public domain, so you can do what you like with it
# but a link to http://scphillips.com would be nice.

# It works on the Raspberry Pi computer with the standard Debian Wheezy OS and
# the 28BJY-48 stepper motor with ULN2003 control board.

from time import sleep
import RPi.GPIO as GPIO

class Motor(object):
    def __init__(self, pins, mode=3):
        """Initialise the motor object.

        pins -- a list of 4 integers referring to the GPIO pins that the IN1, IN2
                IN3 and IN4 pins of the ULN2003 board are wired to
        mode -- the stepping mode to use:
                1: wave drive (not yet implemented)
                2: full step drive
                3: half step drive (default)

        """
        self.P1 = pins[0]
        self.P2 = pins[1]
        self.P3 = pins[2]
        self.P4 = pins[3]
        self.sens=1
        self.mode = mode
        self.deg_per_step = 5.625 / 64  # for half-step drive (mode 3)
        self.steps_per_rev = int(360 / self.deg_per_step)  # 4096
        self.step_angle = 0  # Assume the way it is pointing is zero degrees
        for p in pins:
            GPIO.setup(p, GPIO.OUT)
            GPIO.output(p, 0)

    def _set_rpm(self, rpm):
        """Set the turn speed in RPM."""
        self._rpm = rpm
        # T is the amount of time to stop between signals
        self._T = (60.0 / rpm) / self.steps_per_rev

    # This means you can set "rpm" as if it is an attribute and
    # behind the scenes it sets the _T attribute
    rpm = property(lambda self: self._rpm, _set_rpm)

        
    def setAngle(self, angle):
        self.move_to(angle)

    def move_acw(self,steps):
      if self.mode == 2:
        self._move_acw_2(steps / 8)
      else:
        self._move_acw_3(steps / 8)

    def move_cw(self,steps):
      if self.mode == 2:
        self._move_cw_2(steps / 8)
      else:
        self._move_cw_3(steps / 8)

    def getAngle(self):
        return self.angle
    def up(self,delta):
        self.move_to(self.angle+delta);
    def down(self,delta):
        self.move_to(self.angle-delta);

    def move_to(self, angle):
        self.angle=angle
        if self.sens==-1 :
          angle=360-angle
        """Take the shortest route to a particular angle (degrees)."""
        # Make sure there is a 1:1 mapping between angle and stepper angle
        target_step_angle = 8 * (int(angle / self.deg_per_step) / 8)
        steps = target_step_angle - self.step_angle
        steps = (steps % self.steps_per_rev)
        if steps > self.steps_per_rev / 2:
            steps -= self.steps_per_rev
            print("moving " + str(steps) + " steps")
            self.move_acw(-steps)
        else:
            print ("moving " + str(steps) + " steps")
            self.move_cw(steps)
        self.step_angle = target_step_angle

    def __clear(self):
        GPIO.output(self.P1, 0)
        GPIO.output(self.P2, 0)
        GPIO.output(self.P3, 0)
        GPIO.output(self.P4, 0)

    def _move_acw_2(self, big_steps):
        self.__clear()
        for i in range(int(big_steps)):
            GPIO.output(self.P3, 0)
            GPIO.output(self.P1, 1)
            sleep(self._T * 2)
            GPIO.output(self.P2, 0)
            GPIO.output(self.P4, 1)
            sleep(self._T * 2)
            GPIO.output(self.P1, 0)
            GPIO.output(self.P3, 1)
            sleep(self._T * 2)
            GPIO.output(self.P4, 0)
            GPIO.output(self.P2, 1)
            sleep(self._T * 2)

    def _move_cw_2(self, big_steps):
        self.__clear()
        for i in range(int(big_steps)):
            GPIO.output(self.P4, 0)
            GPIO.output(self.P2, 1)
            sleep(self._T * 2)
            GPIO.output(self.P1, 0)
            GPIO.output(self.P3, 1)
            sleep(self._T * 2)
            GPIO.output(self.P2, 0)
            GPIO.output(self.P4, 1)
            sleep(self._T * 2)
            GPIO.output(self.P3, 0)
            GPIO.output(self.P1, 1)
            sleep(self._T * 2)

    def _move_acw_3(self, big_steps):
        self.__clear()
        for i in range(int(big_steps)):
            GPIO.output(self.P1, 0)
            sleep(self._T)
            GPIO.output(self.P3, 1)
            sleep(self._T)
            GPIO.output(self.P4, 0)
            sleep(self._T)
            GPIO.output(self.P2, 1)
            sleep(self._T)
            GPIO.output(self.P3, 0)
            sleep(self._T)
            GPIO.output(self.P1, 1)
            sleep(self._T)
            GPIO.output(self.P2, 0)
            sleep(self._T)
            GPIO.output(self.P4, 1)
            sleep(self._T)

    def _move_cw_3(self, big_steps):
        self.__clear()
        for i in range(int(big_steps)):
            GPIO.output(self.P3, 0)
            sleep(self._T)
            GPIO.output(self.P1, 1)
            sleep(self._T)
            GPIO.output(self.P4, 0)
            sleep(self._T)
            GPIO.output(self.P2, 1)
            sleep(self._T)
            GPIO.output(self.P1, 0)
            sleep(self._T)
            GPIO.output(self.P3, 1)
            sleep(self._T)
            GPIO.output(self.P2, 0)
            sleep(self._T)
            GPIO.output(self.P4, 1)
            sleep(self._T)
    def stop(self):
        self.move_to(0)
    

if __name__ == "__main__":
    GPIO.setmode(GPIO.BCM)
    m = Motor([6,13,19,26])
    m.rpm = 16
    m.mode = 3
    # m2 = Motor([12,16,20,21])
    # m2.rpm = 16
    # m2.mode = 3
    # m3 = Motor([17,27,22,5])
    # m3.rpm = 16
    # m3.mode = 3
    print("Pause in seconds: " + str(m._T))
    m.move_to(90)
    # m2.move_to(45)
    # m3.move_to(45)
    sleep(1)
    
##    angle=80
##    end=120
##    for i in range(5):
##        m.move_to(end)
##        angle+=10
##        m.move_to(angle)
    angle=80
    for i in range(3):
        angle+=50
        m.move_to(angle)
        # m2.move_to(angle)
        # m3.move_to(angle)
        for j in range(5):
            m.move_to(angle+10)
            m.move_to(angle)
            # m2.move_to(angle+10)
            # m2.move_to(angle)
            # m3.move_to(angle+10)
            # m3.move_to(angle)
            
    sleep(1)
##    m.move_to(270)
##    sleep(1)
##    m.move_to(90)
##    sleep(1)
##    m.move_to(0)
##    sleep(1)
##    m.move_to(270)
    sleep(1)
    m.move_to(0)
    GPIO.cleanup()
