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


To build the body, the head, the arms of your robots, you can use cardboard, or balsa, on thin woods.


On your raspberry, you need to :
- [Install latest Raspbian OS](https://www.raspberrypi.org/downloads/raspbian/)
- [Configure Wifi](http://weworkweplay.com/play/automatically-connect-a-raspberry-pi-to-a-wifi-network/)
- [enable SSH](https://www.raspberrypi.org/documentation/remote-access/ssh/README.md) on your Raspberry pi.
 
*Tips:
To enable SSH and auto configure Wifi when writing raspbian OS onto SD card, 
add an empty file named `ssh` and a file `wpa_suppliant.conf` (cf [Raspbian Stretch Headless Setup Procedure](https://www.raspberrypi.org/forums/viewtopic.php?t=191252))

## Pyroman : Python scripts for Piroman

Connect, via SSH, to your raspberry pi and [configure a shared folder](https://raspberrypihq.com/how-to-share-a-folder-with-a-windows-computer-from-a-raspberry-pi/) `pyroman`:

```
sudo mkdir -m 1777 ~/pyroman
sudo apt-get update
sudo apt-get upgrade
sudo apt-get install samba samba-common-bin
sudo nano /etc/samba/smb.conf
```
Make sure you have:
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
Restart samba
```
sudo service smbd restart
```

Edit you /etc/hosts to change the computer name:
```
sudo nano /etc/hosts
```
And replace `127.0.1.1 raspberrypi` with `127.0.1.1 piroman`

Restart your raspberry:
```
sudo reboot
```

With your file explorer go into \\piroman\pyroman. Copy all files from [Pyroman/script](./Pyroman/script) onto this shared folder

Reconnect with SSH. 

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


## References
- Android Linux / Raspberry Pi Bluetooth communication : http://blog.davidvassallo.me/2014/05/11/android-linux-raspberry-pi-bluetooth-communication/