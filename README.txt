William Berthouex
CSC536 Distributed Systems II
Final Project: Ticketing System

GitHub repository : https://github.com/StateControlled/tokenring.git

Compiled with Scala 3.3.6

How to run:
Start the sbt CLI in the root directory
From the sbt CLI run the commands

    > compile
    > run

The console will offer two options:

    [1] application.client.ClientMain
    [2] application.server.Server

[1] application.client.ClientMain   will run a client
[2] application.server.Server		will run a server

Run one of each in separate consoles.

Client commands:
LIST    - queries the Master for a list of events on sale
BUY     - sends a BUY request to a Kiosk
ORDERS  - lists all purchased tickets
EXIT    - terminates the client

Server commands:
LIST    - prints event sales data from the Master and all Kiosks to the console
EXIT    - terminates the system
