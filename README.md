# Abusive Charge Point

This program mimics a OCPP 1.5-j charge point. It will send random messages and tries to match 
the received messages to active calls (using the WAMP keys). It can send the following OCPP 
messages:
 
* `BootNotification`
* `StartTransaction` 
* `StopTransaction`
* `Heartbeat`
* `MeterValues`
* `StatusNotification`

## Usage

Use `lein run` to enter the REPL. Use `help` to retrieve all possible commmands.

Shortcut: `lein run "pool-config.json"` to start running a set of charge points that are 
defined in a config file. 
