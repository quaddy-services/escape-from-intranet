# escape-from-intranet
Escape out of intranet without dealing with authentication of corporate proxy

Start with -DdefaultLogLevel=debug or trace to see more log-entries in ${java.io.tmpdir}/escape-from-intranet.log

To run it
- make a "release" from source: just use mvn package and take target\escape-from-intranet.jar
- download a jar from [releases](https://github.com/quaddy-services/escape-from-intranet/releases) and run it
- download the windows setup from [releases](https://github.com/quaddy-services/escape-from-intranet/releases)

(in case all is configured the application minizes to tray)

# Example settings

See ![Screenshot](https://github.com/quaddy-services/escape-from-intranet/raw/master/src/site/resources/example-screenshot.png "Screenshot")

# escape-from-intranet-setup

In case you receive "Windows Defender SmartScreen prevented an unrecognized app from starting." after executing the setup

![Setup fails](https://github.com/quaddy-services/escape-from-intranet/raw/master/src/site/resources/windows10-protects-your-pc.png "Setup fails")

you need to trust the exe file by opening properties and "Unblock" the setup:

![unblock-setup](https://github.com/quaddy-services/escape-from-intranet/raw/master/src/site/resources/unblock-setup.png "unblock-setup")


See http://quaddy-services.de/
