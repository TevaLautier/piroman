# Piroman : Raspberry *Pi* *ro*bot for Weather and Video Surveillance 

<img src="piroman.jpg" alt="Image" style="width: 200px;"/>
This project allows to create a raspberry pi robot controlled by an android application, over bluetooth.
This robot can gives information on weather (temperature, pression, humidity), and is a also a camera surveillance 


It can also be transformed into a [Retropi](https://retropie.org.uk/) console. You just need to replace its SD card with a Retropie one, and connect joysticks.

<img src="piroman-bomberman.jpg" alt="Image" style="width: 300px;"/>.


## Prerequisites

You need to have
- A Raspberry pi 3, with its alimentation and with a raspbian OS
- The [OSOYOO starter kit](https://www.amazon.fr/OSOYOO-Raspberry-Electronique-explorateurs-amateurs/dp/B074YZMRC1)
- A [night vision camera module](https://www.amazon.fr/gp/product/B071J14338)
	- with a longer cable : [30cm](https://www.amazon.fr/gp/product/B01NAXKTDP)  or [20cm](https://www.amazon.fr/gp/product/B00RMV2L0M)
- A raspberry compatible [Wifi Dongle](https://www.amazon.fr/gp/product/B003MTTJOY)


To build the body, the head, the arms of your robots, you can use cardboard, balsa, on thin woods.


On your raspberry, you need to :
- [Install latest Raspbian OS](https://www.raspberrypi.org/downloads/raspbian/)
- [Configure Wifi](http://weworkweplay.com/play/automatically-connect-a-raspberry-pi-to-a-wifi-network/)
- [enable SSH](https://www.raspberrypi.org/documentation/remote-access/ssh/README.md) on your Raspberry pi.
 
*Tips:
To enable SSH and auto configure Wifi, write raspbian OS image onto your SD card, and in `boot` disk,  
add an empty file named `ssh` and a file `wpa_suppliant.conf` (cf [Raspbian Stretch Headless Setup Procedure](https://www.raspberrypi.org/forums/viewtopic.php?t=191252))

## Pyroman : Python scripts for Piroman

### Configure shared folder

Connect, via SSH, to your raspberry pi and [configure a shared folder](https://raspberrypihq.com/how-to-share-a-folder-with-a-windows-computer-from-a-raspberry-pi/) `pyroman`:

```
sudo mkdir -m 1777 ~/pyroman
sudo apt-get update
sudo apt-get upgrade
sudo apt-get install samba samba-common-bin
```

In `/etc/samba/smb.conf`, make sure you have:
```
workgroup = WORKGROUP
wins support = yes
```
And add lines at the end: 
```
[pyroman]
        comment = Pyroman Shared Folder
        path = /home/pi/pyroman
        browsable = yes
        guest ok = yes
        writable = yes
        create mask = 0644
        directory mask = 0755
        force create mask = 0644
        force directory mask = 0755
        force user = pi
        force group = pi
```
### Change computer name

Edit you /etc/hosts to change the computer name:
```
sudo nano /etc/hosts
```
And replace `127.0.1.1 raspberrypi` with `127.0.1.1 piroman`

### Configure bluetooth

We need to install bluetooth (cf [Tutorial](https://circuitdigest.com/microcontroller-projects/controlling-raspberry-pi-gpio-using-android-app-over-bluetooth)
```
sudo apt-get install bluetooth blueman bluez
sudo reboot
sudo apt-get install python-bluetooth
sudo apt-get install python-rpi.gpio
```

Activate bluetooth on your raspberry
```
sudo bluetoothctl
[bluetooth]# power on
[bluetooth]# agent on
[bluetooth]# discoverable on
[bluetooth]# pairable on
[bluetooth]# scan on
[bluetooth]# quit	
```

To rename the bluetooth device name, create file `/etc/machine-info` and put inside:
```
PRETTY_HOSTNAME=device-name
```

Restart your raspberry:
```
sudo reboot
```

### Motion

Motion is a tool to detect motion with camera. We need to [install it](https://raspbian-france.fr/video-surveillance-raspberry-pi-camera/) :
```
sudo apt-get install motion
```

To enable motion to work with raspberry camera, yu need to execute :
```
sudo modprobe bcm2835-v4l2
echo "bcm2835-v4l2" | sudo tee -a /etc/modules
```


Edit file `/etc/default/motion` to enable motion as a daemon
```
start_motion_daemon=yes
```
Edit `/etc/motion/motion.conf` to change these lines, or use the provided [motion.conf](./conf/motion.conf)
```
daemon on

logfile /home/pi/pyroman/log/motion.log

width 640
height 360

pre_capture 2
post_capture 2

max_movie_time 300

output_pictures best

ffmpeg_video_codec msmpeg4

locate_motion_mode on

target_dir /home/pi/pyroman/camera

snapshot_filename %v-%Y%m%d-%H%M%S-snapshot
picture_filename %v-%Y%m%d-%H%M%S-%q
movie_filename %v-%Y%m%d-%H%M%S

# Restrict stream connections to localhost only (default: on)
stream_localhost off

on_movie_start  /home/pi/pyroman/event/onMovieStart.sh 

```

To allow motion to write log, videos, images, we need to add user motion to pi group, and change some file rights
```
sudo adduser motion pi
chmod -R g+rw ~/pyroman/log
mkdir camera
chmod -R g+rw ~/pyroman/camera
```

We also need to activate camera on Pi
```
sudo raspi-config
```
Go into `Interfacing Options->Camera` and enable it

Restart  motion
```
sudo service motion restart
```
To verify if motion works fine, go to [http://piroman:8081](http://piroman:8081)



### Add Pyroman scripts

With your file explorer go into \\piroman\pyroman. Copy all files from [Pyroman/script](./Pyroman/script) onto this shared folder





## References
- Android Linux / Raspberry Pi Bluetooth communication : http://blog.davidvassallo.me/2014/05/11/android-linux-raspberry-pi-bluetooth-communication/