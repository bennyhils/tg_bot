#!/bin/bash
cd ..
mv target/bot-1.0.jar distr/bot.jar
docker build -t bot distr
docker login
docker tag bot bennyhils/bot:coffee_24.11.26
docker push bennyhils/bot:coffee_24.11.26
