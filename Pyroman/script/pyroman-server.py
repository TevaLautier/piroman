#!/usr/bin/python

import logging
import logging.handlers
import argparse
import sys
import os
import json
import time
from bluetooth import *
from pyroman import Robot
import codecs


robot=Robot();
scenaariFile=fn = os.path.join(os.path.dirname(__file__), 'scenari.json')
with codecs.open(scenaariFile,'r',encoding='utf8') as f:
    scenaris = json.loads(f.read())



class LoggerHelper(object):
    def __init__(self, logger, level):
        self.logger = logger
        self.level = level

    def write(self, message):
        if message.rstrip() != "":
            self.logger.log(self.level, message.rstrip())


def setup_logging():
    # Default logging settings
    LOG_FILE = "/home/pi/shared/log/raspibtsrv.log"
    LOG_LEVEL = logging.INFO

    # Define and parse command line arguments
    argp = argparse.ArgumentParser(description="Raspberry PI Bluetooth Server")
    argp.add_argument("-l", "--log", help="log (default '" + LOG_FILE + "')")

    # Grab the log file from arguments
    args = argp.parse_args()
    if args.log:
        LOG_FILE = args.log

    # Setup the logger
    logger = logging.getLogger(__name__)
    # Set the log level
    logger.setLevel(LOG_LEVEL)
    # Make a rolling event log that resets at midnight and backs-up every 3 days
    handler = logging.handlers.TimedRotatingFileHandler(LOG_FILE,
        when="midnight",
        backupCount=3)

    # Log messages should include time stamp and log level
    formatter = logging.Formatter('%(asctime)s %(levelname)-8s %(message)s')
    # Attach the formatter to the handler
    handler.setFormatter(formatter)
    # Attach the handler to the logger
    logger.addHandler(handler)

    # Replace stdout with logging to file at INFO level
    sys.stdout = LoggerHelper(logger, logging.INFO)
    # Replace stderr with logging to file at ERROR level
    sys.stderr = LoggerHelper(logger, logging.ERROR)

def handleRequest(data):
            print "req="+data
            response=""
            if data == "getop":
                response = "op:%s" % ",".join(operations)
            elif data.startswith("left:") or data.startswith("right:"):
                #left:up:20
                args=data.split(":")
                leftright=args[0];
                updown=args[1]
                amount=int(args[2])
                bras=robot.gauche if leftright=="left" else robot.droite
                brasN="g" if leftright=="left" else "d"
                if updown=="up":
                    bras.up(amount)
                elif updown=="down":
                    bras.down(amount)
                elif updown=="set":
                    bras.setAngle(amount)

                response = "msg:"+leftright+" "+brasN+" Arm Moved "+str(amount)+" "+str(robot.droite==bras)
            elif data.startswith("head:") :
                #head:left:20
                args=data.split(":")
                updown=args[1]
                amount=int(args[2])
                if updown=="left":
                    robot.tete.up(amount)
                elif updown=="right":
                    robot.tete.down(amount)
                elif updown=="set":
                    robot.tete.setAngle(amount)

                response = "msg:Head Moved "+str(amount) 
            elif data.startswith("line"):
                # line:0:Hello World
                args=data.split(":")
                numero=int(args[1])
                msg=args[2] 
                robot.lcd.line(numero,msg);
                response="msg:line "+str(numero)+" setted to :"+msg
            elif data.startswith("lcd:"):
                # lcd:off  lcd:on
                args=data.split(":")
                off="off"==args[1];
                if off:
                  robot.lcd.stopBackLight();
                else:
                    robot.lcd.start();
                response="msg:Lcd :"+str(off);
            elif data.startswith("scenario:"):
                # scenario:get:Makarena
                args=data.split(":")
                op=args[1]
                name=args[2]
                s={};
                for scenario in scenaris:
                   if scenario["name"]==name:
                        s=scenario
                print "op"+op
                
                if s=={} and op != "put" and name != "*":
                    response="msg:Scenario not found :"+name;
                else:
                    if op=="get" :
                        if(name=="*"):
                            response="msg:"+json.dumps(scenaris)
                        else:
                            response="msg:"+json.dumps(s)
                    elif op=="exec":
                        for step in s["steps"]:
                            handleRequest(step)
                    elif op=="set":
                        # replace by current
                        scenaris.remove(s)
                        print "load:"+args[3]
                        s=json.loads(args[3])
                        scenaris.append(s)
                        
                        #save scenaris
                        with codecs.open(scenaariFile,'w',encoding='utf8') as f:
                            f.write(json.dumps(scenaris))

            elif data.startswith("action"):
                # action:meteo
                args=data.split(":")
                action=args[1]
                if action == "meteo":
                    temp=robot.meteo.read_temperature()
                    temp2 = robot.meteo.read_temperature2()
                    pressure = robot.meteo.read_pressure()/100
                    altitude = robot.meteo.read_altitude()
                    humidity = robot.meteo.read_humidity()
                    robot.lcd.line(0,"%.1fC" % temp+" / "+"%.2fC" % temp2)
                    robot.lcd.line(1,"%.0fhPa" % pressure+" %s%%" % humidity)
                if action == "coucou":
                    robot.lcd.line(0,"     COUCOU   ")
                    robot.lcd.line(1,"     :):):)   ")

                    robot.droite.setAngle(80)
                    robot.gauche.setAngle(120)
                    robot.tete.up(10)
                    for i in range(10):
                        robot.tete.down(20)
                        robot.tete.up(20)
                        robot.droite.setAngle(90)
                        robot.droite.setAngle(80)
                        robot.gauche.setAngle(110)
                        robot.gauche.setAngle(120)
                    robot.droite.setAngle(0)
                    robot.gauche.setAngle(0)
                    robot.tete.down(10)
                        
                response="msg:Action executed"
            elif data == "example":
                response = "msg:This is an example"
            # Insert more here
            else:
                response = "msg:Not supported"
            print data+"="+response
            return response
# Main loop
def main():
    print("==============================")
    # Setup logging
    setup_logging()

    # We need to wait until Bluetooth init is done
    time.sleep(10)

    # Make device visible
    os.system("hciconfig hci0 piscan")

    # Create a new server socket using RFCOMM protocol
    server_sock = BluetoothSocket(RFCOMM)
    # Bind to any port
    server_sock.bind(("", PORT_ANY))
    # Start listening
    server_sock.listen(1)

    # Get the port the server socket is listening
    port = server_sock.getsockname()[1]

    # The service UUID to advertise
    uuid = "7be1fcb3-5776-42fb-91fd-2ee7b5bbb86d"

    # Start advertising the service
    advertise_service(server_sock, "PiromanSrv",
                       service_id=uuid,
                       service_classes=[uuid, SERIAL_PORT_CLASS],
                       profiles=[SERIAL_PORT_PROFILE])

    # These are the operations the service supports
    # Feel free to add more
    operations = ["ping", "example"]
    var1 = 'Hello World!'
    var2 = "Python Programming"

    print("var1[0]: ", var1[0])
    print ("var2[1:5]: ", var2[1:5])
    #eteint la lumiere du lcd au demarrage
    robot.stop();
    print("==============================")
    #print handleRequest("scenario:get:*")
    #print handleRequest("scenario:get:eee")
    #print handleRequest("scenario:set:eee:{'name': 'Coucou','steps': ['lcd:off']}")

    # Main Bluetooth server loop
    while True:

        print "Waiting for connection on RFCOMM channel %d" % port

        try:
            client_sock = None

            # This will block until we get a new connection
            client_sock, client_info = server_sock.accept()
            print "Accepted connection from ", client_info

            # Read the data sent by the client
            data = client_sock.recv(1024)
            if len(data) == 0:
                break

            print "Received [%s]" % data

            response=handleRequest(data)

            
            client_sock.send(response)
            print "Sent back [%s]" % response

        except IOError:
            pass

        except KeyboardInterrupt:

            if client_sock is not None:
                client_sock.close()

            server_sock.close()

            print "Server going down"
            break
        # finally:
        #     robot.stopAll()
main()
