#!/bin/bash
cd ..
mvn clean install
mv target/bot-1.0.jar distr/bot.jar
docker build -t bot distr
docker login
docker tag bot bennyhils/bot
docker push bennyhils/bot
