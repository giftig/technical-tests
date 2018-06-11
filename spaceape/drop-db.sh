#!/bin/bash

docker-compose stop couchdb
docker-compose rm -f couchdb
docker-compose up -d couchdb
