# Using Firestore as data repository


## One-time Set-up of Firestore Project

First you need to have created a Google Cloud Platform project with Firestore AND created a Firebase project "linked to" said GCP project.

## One-time Set-up of Local Firebase Emulator

    $ firebase init

When asked, enable these features:
 * Emulators
 * Firestore
 
Other input to provide (defaults are usually good):
 * Set up a default project: No (not necessary if only testing locally)
 * Firestore Rules file: firestore.rules
 * Firestore indexes file: firestore.indexes.json
 * Which Firebase emulators do you want to set up? Press Space to select emulators, then Enter to confirm your choices. Firestore Emulator
 * Firestore emulator port: 8081
 * Enable the Emulator UI? Yes

## Run things locally using Docker

### Start emulator

You can start the emulator(s) like this:

    $ firebase emulators:start

Note: You can use the `emulators:export` command and the `--import` argument to export and import data, respectively:

    $ firebase emulators:export ./firestore-export-baseline
    $ firebase emulators:start --import=./firestore-export-baseline

### Build service

Build the Docker image:

    $ docker build --tag names:latest .

### Start service

Create a `.env` file with password and other secrets. You can copy the `.env.sample` file and modify it.

Start the container and reference the `.env` file:

    $ docker run -p 8443:8443 --env-file ./names-backend-service/.env names:latest

Note: Make sure the port number in the `-p` argument matches that in your `.env` file.

### Load data

(To be written.)

### Start front-end

(To be written.)
